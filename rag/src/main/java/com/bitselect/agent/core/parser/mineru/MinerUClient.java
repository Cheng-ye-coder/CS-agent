package com.bitselect.agent.core.parser.mineru;

import com.bitselect.agent.framework.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 【文件用途】MinerU SaaS HTTP 客户端：封装与 MinerU 官方 API 的所有交互。
 *
 * 【为什么存在】
 * - MinerU 的 API 有 4 个操作，分散调用会重复处理鉴权、错误、JSON 解析
 * - 集中一处便于统一升级（如 MinerU 改了 API 版本）
 *
 * 【4 个核心方法】
 * 1. requestUpload：申请上传链接，返回 batch_id + 上传 URL
 * 2. uploadFile：把文件字节 PUT 上传到 MinerU OSS
 * 3. queryResult：查询任务状态（轮询用）
 * 4. downloadZip：下载结果 zip 字节流
 *
 * 【鉴权】
 * - 申请上传链接与查询结果：HTTP header Authorization: Bearer <api-key>
 * - 上传与下载：用预签名 URL，不带 Authorization
 *
 * 【响应解析】
 * 用 JsonNode 兼容 MinerU 字段细节变化，只读关心的字段。
 *
 * 【被谁引用】MinerUDocumentParser、MinerUPollingExecutor。
 */
@Slf4j
@Component
public class MinerUClient {

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MinerUProperties properties;

    public MinerUClient(@Qualifier("syncHttpClient") OkHttpClient httpClient,
                        ObjectMapper objectMapper,
                        MinerUProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 【方法用途】申请上传链接（本地文件批量上传解析的第一步）。
     *
     * 只提交文件元信息，MinerU 返回 batch_id 与预签名上传 URL。
     * 拿到 URL 后须调 uploadFile 把文件字节 PUT 上去，MinerU 才会自动提交解析。
     */
    public BatchUploadTicket requestUpload(BatchSubmitRequest request) {
        requireApiKey();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("enable_formula", request.enableFormula());
        body.put("enable_table", request.enableTable());
        body.put("language", request.language() == null ? "ch" : request.language());

        ArrayNode files = body.putArray("files");
        ObjectNode file = files.addObject();
        if (request.fileName() != null) {
            file.put("name", request.fileName());
        }
        file.put("is_ocr", request.isOcr());
        if (request.dataId() != null) {
            file.put("data_id", request.dataId());
        }

        String url = properties.getApiUrl() + "/file-urls/batch";
        Request httpRequest = newJsonPost(url, body.toString());

        JsonNode root = executeAndParse(httpRequest, "requestUpload");
        ensureSuccess(root, "requestUpload");

        JsonNode data = root.path("data");
        String batchId = data.path("batch_id").asText(null);
        if (batchId == null || batchId.isBlank()) {
            throw new ServiceException("MinerU requestUpload 返回缺少 batch_id, body=" + root);
        }

        JsonNode fileUrls = data.path("file_urls");
        if (!fileUrls.isArray() || fileUrls.isEmpty()) {
            throw new ServiceException("MinerU requestUpload 返回缺少 file_urls, body=" + root);
        }
        String uploadUrl = fileUrls.get(0).asText(null);
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new ServiceException("MinerU requestUpload 返回的 file_urls[0] 为空, body=" + root);
        }

        log.info("MinerU 申请上传链接成功 batchId={} fileName={}", batchId, request.fileName());
        return new BatchUploadTicket(batchId, uploadUrl);
    }

    /**
     * 【方法用途】上传文件字节到 MinerU 预签名 URL（第二步）。
     *
     * 目标是 OSS 预签名 PUT 链接，按 MinerU 官方要求不设 Content-Type、不带 Authorization。
     * 上传成功后 MinerU 自动探测并提交解析任务，随后可 queryResult 轮询。
     */
    public void uploadFile(String uploadUrl, byte[] content) {
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new ServiceException("uploadUrl 不能为空");
        }
        if (content == null || content.length == 0) {
            throw new ServiceException("上传字节不能为空");
        }
        Request httpRequest = new Request.Builder()
                .url(uploadUrl)
                .put(RequestBody.create(content, null))
                .build();
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String body = readBodySafe(response);
                throw new ServiceException("MinerU uploadFile 失败 code=" + response.code() + " body=" + body);
            }
            log.info("MinerU 文件上传成功 size={} url={}", content.length, uploadUrl);
        } catch (IOException e) {
            throw new ServiceException("MinerU uploadFile 网络异常: " + e.getMessage());
        }
    }

    /**
     * 【方法用途】查询任务状态。
     */
    public MinerUStatus queryResult(String batchId) {
        requireApiKey();
        if (batchId == null || batchId.isBlank()) {
            throw new ServiceException("batchId 不能为空");
        }

        // MinerU v4 按 batch_id 查询结果：batch_id 是路径段
        String url = properties.getApiUrl() + "/extract-results/batch/" + batchId;
        Request httpRequest = newGet(url);

        JsonNode root = executeAndParse(httpRequest, "queryResult");
        ensureSuccess(root, "queryResult");

        JsonNode data = root.path("data");
        JsonNode extractResult = data.path("extract_result");
        if (!extractResult.isArray() || extractResult.isEmpty()) {
            // 任务可能仍在排队，暂无 extract_result
            return new MinerUStatus(MinerUTaskState.RUNNING, null, null);
        }

        JsonNode item = extractResult.get(0);
        String stateRaw = item.path("state").asText(null);
        MinerUTaskState state = MinerUTaskState.parse(stateRaw);
        String zipUrl = item.path("full_zip_url").asText(null);
        String errMsg = item.path("err_msg").asText(null);

        return new MinerUStatus(state, zipUrl, errMsg);
    }

    /**
     * 【方法用途】下载结果 zip 字节流。
     *
     * 此 URL 通常是一次性预签名 URL，有时效；拿到 MinerUStatus.zipUrl 后立即下载。
     */
    public byte[] downloadZip(String zipUrl) {
        if (zipUrl == null || zipUrl.isBlank()) {
            throw new ServiceException("zipUrl 不能为空");
        }
        Request httpRequest = new Request.Builder().url(zipUrl).get().build();
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String body = readBodySafe(response);
                throw new ServiceException("MinerU downloadZip 失败 code=" + response.code() + " body=" + body);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ServiceException("MinerU downloadZip 响应体为空");
            }
            return body.bytes();
        } catch (IOException e) {
            throw new ServiceException("MinerU downloadZip 网络异常: " + e.getMessage());
        }
    }

    // ============== private helpers ==============

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ServiceException("MinerU api-key 未配置，请设置环境变量 MINERU_API_KEY");
        }
    }

    private Request newJsonPost(String url, String jsonBody) {
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON_MEDIA))
                .build();
    }

    private Request newGet(String url) {
        return new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .get()
                .build();
    }

    private JsonNode executeAndParse(Request request, String opName) {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = readBodySafe(response);
            if (!response.isSuccessful()) {
                throw new ServiceException(String.format(
                        "MinerU %s HTTP 异常 code=%d, body=%s", opName, response.code(), body));
            }
            try {
                return objectMapper.readTree(body);
            } catch (IOException e) {
                throw new ServiceException("MinerU " + opName + " 响应非 JSON: " + body);
            }
        } catch (IOException e) {
            throw new ServiceException("MinerU " + opName + " 网络异常: " + e.getMessage());
        }
    }

    /**
     * 检查 MinerU 业务码，非 0 抛错。
     *
     * MinerU 标准响应格式 {"code":0,"msg":"ok","data":{...}}
     */
    private void ensureSuccess(JsonNode root, String opName) {
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText("unknown");
            throw new ServiceException(String.format(
                    "MinerU %s 业务异常 code=%d msg=%s", opName, code, msg));
        }
    }

    private String readBodySafe(Response response) {
        try {
            ResponseBody body = response.body();
            return body == null ? "" : body.string();
        } catch (IOException e) {
            return "";
        }
    }
}