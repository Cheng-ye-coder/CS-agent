

package com.bitselect.agent.infra.embedding;

import cn.hutool.core.collection.CollUtil;
import com.bitselect.agent.infra.config.AIModelProperties;
import com.bitselect.agent.infra.enums.ModelCapability;
import com.bitselect.agent.infra.http.HttpMediaTypes;
import com.bitselect.agent.infra.http.HttpResponseHelper;
import com.bitselect.agent.infra.http.ModelClientErrorType;
import com.bitselect.agent.infra.http.ModelClientException;
import com.bitselect.agent.infra.http.ModelUrlResolver;
import com.bitselect.agent.infra.model.ModelTarget;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OpenAI 兼容协议 EmbeddingClient 抽象基类
 */
@Slf4j
public abstract class AbstractOpenAIStyleEmbeddingClient implements EmbeddingClient {

    protected final OkHttpClient httpClient;

    protected AbstractOpenAIStyleEmbeddingClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected boolean requiresApiKey() {
        return true;
    }

    protected void customizeRequestBody(JsonObject body, ModelTarget target) {
        body.addProperty("encoding_format", "float");
    }

    protected int maxBatchSize() {
        return 0;
    }

    @Override
    public List<Float> embed(String text, ModelTarget target) {
        List<List<Float>> result = doEmbed(List.of(text), target);
        return result.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts, ModelTarget target) {
        if (CollUtil.isEmpty(texts)) {
            return Collections.emptyList();
        }
        int batch = maxBatchSize();
        if (batch <= 0 || texts.size() <= batch) {
            return doEmbed(texts, target);
        }

        List<List<Float>> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        for (int i = 0, n = texts.size(); i < n; i += batch) {
            int end = Math.min(i + batch, n);
            List<String> slice = texts.subList(i, end);
            List<List<Float>> part = doEmbed(slice, target);
            for (int k = 0; k < part.size(); k++) {
                results.set(i + k, part.get(k));
            }
        }
        return results;
    }

    protected List<List<Float>> doEmbed(List<String> texts, ModelTarget target) {
        AIModelProperties.ProviderConfig provider = HttpResponseHelper.requireProvider(target, provider());
        if (requiresApiKey()) {
            HttpResponseHelper.requireApiKey(provider, provider());
        }

        String url = ModelUrlResolver.resolveUrl(provider, target.candidate(), ModelCapability.EMBEDDING);

        String model = HttpResponseHelper.requireModel(target, provider());

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        JsonArray inputArray = new JsonArray();
        for (String text : texts) {
            inputArray.add(text);
        }
        body.add("input", inputArray);
        body.addProperty("dimensions", target.candidate().getDimension());
        customizeRequestBody(body, target);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), HttpMediaTypes.JSON));
        if (requiresApiKey()) {
            requestBuilder.addHeader("Authorization", "Bearer " + provider.getApiKey());
        }
        Request request = requestBuilder.build();

        long startTime = System.currentTimeMillis();

        JsonObject json;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = HttpResponseHelper.readBody(response.body());
                log.warn("{} embedding 请求失败 - 模型：{}，条数：{}，耗时：{}ms，status={}, body={}",
                        provider(), model, texts.size(), System.currentTimeMillis() - startTime, response.code(), errBody);
                throw new ModelClientException(
                        provider() + " embedding 请求失败: HTTP " + response.code(),
                        ModelClientErrorType.fromHttpStatus(response.code()),
                        response.code()
                );
            }
            json = HttpResponseHelper.parseJson(response.body(), provider());
        } catch (IOException e) {
            log.warn("{} embedding 请求异常 - 模型：{}，条数：{}，耗时：{}ms，原因：{}",
                    provider(), model, texts.size(), System.currentTimeMillis() - startTime, e.getMessage());
            throw new ModelClientException(
                    provider() + " embedding 请求失败: " + e.getMessage(),
                    ModelClientErrorType.NETWORK_ERROR, null, e);
        }

        if (json.has("error")) {
            JsonObject err = json.getAsJsonObject("error");
            String code = err.has("code") ? err.get("code").getAsString() : "unknown";
            String msg = err.has("message") ? err.get("message").getAsString() : "unknown";
            throw new ModelClientException(
                    provider() + " embedding 错误: " + code + " - " + msg,
                    ModelClientErrorType.PROVIDER_ERROR, null);
        }

        log.info("{} embedding 完成 - 模型：{}，条数：{}，耗时：{}ms",
                provider(), model, texts.size(), System.currentTimeMillis() - startTime);

        JsonArray data = json.getAsJsonArray("data");
        if (data == null || data.isEmpty()) {
            throw new ModelClientException(
                    provider() + " embedding 响应中缺少 data 数组",
                    ModelClientErrorType.INVALID_RESPONSE, null);
        }

        List<List<Float>> results = new ArrayList<>(data.size());
        for (JsonElement el : data) {
            JsonObject obj = el.getAsJsonObject();
            JsonArray emb = obj.getAsJsonArray("embedding");
            if (emb == null || emb.isEmpty()) {
                throw new ModelClientException(
                        provider() + " embedding 响应中缺少 embedding 字段",
                        ModelClientErrorType.INVALID_RESPONSE, null);
            }
            List<Float> vector = new ArrayList<>(emb.size());
            for (JsonElement v : emb) {
                vector.add(v.getAsFloat());
            }
            results.add(vector);
        }

        return results;
    }
}