package com.bitselect.agent.ingestion.util;

import com.bitselect.agent.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 【文件用途】HTTP 请求工具类：统一封装 GET / HEAD / 流式读取三类远程文件访问。
 *
 * 【为什么存在】
 * - 远程文档导入与定时刷新都需要下载外部 URL 的文件
 * - 直接裸用 OkHttp 会重复处理响应头解析、文件名提取、限流等逻辑
 * - 集中一处便于统一管控超时、大小限制、错误转换
 *
 * 【三个核心方法】
 * 1. get / getWithLimit：一次性读取整个响应体
 * 2. openStream：返回流式响应，适合大文件边下边处理
 * 3. head：只取响应头（ETag / Last-Modified / Content-Length），用于变更检测
 *
 * 【关键设计】
 * - 所有方法支持 maxBytes 限制，防止恶意大文件打爆内存
 * - 文件名从 Content-Disposition 提取，提取失败时回退到 URL 路径末段
 * - 用 syncHttpClient（同步客户端），有超时保护
 * - 限流用包装 InputStream 实现，读取时逐次累计
 *
 * 【被谁引用】
 * - knowledge/handler/RemoteFileFetcher（远程文件拉取）
 * - 其他需要 HTTP 访问的组件
 */
@Component
@RequiredArgsConstructor
public class HttpClientHelper {

    @Qualifier("syncHttpClient")
    private final OkHttpClient client;

    /**
     * 【方法用途】GET 请求，一次性读取整个响应体。
     */
    public HttpFetchResponse get(String url, Map<String, String> headers) {
        return doGet(url, headers, -1);
    }

    /**
     * 【方法用途】GET 请求，带大小限制，超过 maxBytes 抛异常。
     */
    public HttpFetchResponse getWithLimit(String url, Map<String, String> headers, long maxBytes) {
        return doGet(url, headers, maxBytes);
    }

    /**
     * 【方法用途】流式读取响应，调用方负责关闭返回的 HttpFetchStream。
     *
     * 适合大文件：边读边处理，不把整个响应加载到内存。
     */
    public HttpFetchStream openStream(String url, Map<String, String> headers, long maxBytes) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        try {
            Response response = client.newCall(builder.get().build()).execute();
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                throw new ServiceException("网络请求失败: " + response.code() + " " + body);
            }
            ResponseBody responseBody = response.body();
            String contentType = response.header("Content-Type");
            String disposition = response.header("Content-Disposition");
            String fileName = resolveFileName(disposition, url);
            String etag = response.header("ETag");
            String lastModified = response.header("Last-Modified");
            Long contentLength = parseContentLength(response.header("Content-Length"));
            if (maxBytes > 0 && contentLength != null && contentLength > maxBytes) {
                response.close();
                throw new ServiceException("文件大小超过限制: " + maxBytes + " bytes");
            }
            InputStream bodyStream = responseBody == null
                    ? InputStream.nullInputStream()
                    : wrapWithLimit(responseBody.byteStream(), maxBytes);
            return new HttpFetchStream(response, bodyStream, contentType, fileName, etag, lastModified, contentLength);
        } catch (IOException e) {
            throw new ServiceException("网络请求失败: " + e.getMessage());
        }
    }

    private HttpFetchResponse doGet(String url, Map<String, String> headers, long maxBytes) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        try (Response response = client.newCall(builder.get().build()).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new ServiceException("网络请求失败: " + response.code() + " " + body);
            }
            String contentType = response.header("Content-Type");
            String disposition = response.header("Content-Disposition");
            String fileName = resolveFileName(disposition, url);
            String etag = response.header("ETag");
            String lastModified = response.header("Last-Modified");
            Long contentLength = parseContentLength(response.header("Content-Length"));
            if (maxBytes > 0 && contentLength != null && contentLength > maxBytes) {
                throw new ServiceException("文件大小超过限制: " + maxBytes + " bytes");
            }

            byte[] bytes;
            if (response.body() == null) {
                bytes = new byte[0];
            } else if (maxBytes > 0) {
                bytes = readWithLimit(response.body().byteStream(), maxBytes);
            } else {
                bytes = response.body().bytes();
            }
            return new HttpFetchResponse(bytes, contentType, fileName, etag, lastModified, contentLength);
        } catch (IOException e) {
            throw new ServiceException("网络请求失败: " + e.getMessage());
        }
    }

    /**
     * 【方法用途】HEAD 请求：只取响应头，用于变更检测（比对 ETag / Last-Modified）。
     */
    public HttpHeadResponse head(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        try (Response response = client.newCall(builder.head().build()).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException("网络请求失败: " + response.code());
            }
            String contentType = response.header("Content-Type");
            String disposition = response.header("Content-Disposition");
            String fileName = resolveFileName(disposition, url);
            String etag = response.header("ETag");
            String lastModified = response.header("Last-Modified");
            Long contentLength = parseContentLength(response.header("Content-Length"));
            return new HttpHeadResponse(etag, lastModified, contentType, contentLength, fileName);
        } catch (IOException e) {
            throw new ServiceException("网络请求失败: " + e.getMessage());
        }
    }

    /**
     * 【方法用途】从 Content-Disposition 提取文件名，失败时回退到 URL 路径末段。
     */
    private String resolveFileName(String disposition, String url) {
        if (disposition != null) {
            String[] parts = disposition.split(";");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("filename=")) {
                    String raw = trimmed.substring("filename=".length()).trim();
                    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
                        raw = raw.substring(1, raw.length() - 1);
                    }
                    return decode(raw);
                }
            }
        }
        try {
            URL parsed = new URL(url);
            String path = parsed.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            int idx = path.lastIndexOf('/');
            return idx >= 0 ? path.substring(idx + 1) : path;
        } catch (Exception e) {
            return null;
        }
    }

    private String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private Long parseContentLength(String header) {
        if (header == null) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private byte[] readWithLimit(InputStream inputStream, long maxBytes) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (maxBytes > 0 && total > maxBytes) {
                    throw new ServiceException("文件大小超过限制: " + maxBytes + " bytes");
                }
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    /**
     * 【方法用途】包装 InputStream 实现流式限流：读取时逐次累计，超限抛异常。
     */
    private InputStream wrapWithLimit(InputStream inputStream, long maxBytes) {
        if (maxBytes <= 0) {
            return inputStream;
        }
        return new InputStream() {
            private long total;

            @Override
            public int read() throws IOException {
                int value = inputStream.read();
                if (value != -1) {
                    ensureWithinLimit(1);
                }
                return value;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int count = inputStream.read(b, off, len);
                if (count > 0) {
                    ensureWithinLimit(count);
                }
                return count;
            }

            @Override
            public void close() throws IOException {
                inputStream.close();
            }

            private void ensureWithinLimit(int delta) {
                total += delta;
                if (total > maxBytes) {
                    throw new ServiceException("文件大小超过限制: " + maxBytes + " bytes");
                }
            }
        };
    }

    /**
     * 【内部结构】GET 响应：字节内容 + 元数据。
     */
    public record HttpFetchResponse(byte[] body,
                                    String contentType,
                                    String fileName,
                                    String etag,
                                    String lastModified,
                                    Long contentLength) {
    }

    /**
     * 【内部结构】流式响应：持有 OkHttp Response，调用方需 close。
     */
    public record HttpFetchStream(Response response,
                                  InputStream bodyStream,
                                  String contentType,
                                  String fileName,
                                  String etag,
                                  String lastModified,
                                  Long contentLength) implements AutoCloseable {

        @Override
        public void close() {
            response.close();
        }
    }

    /**
     * 【内部结构】HEAD 响应：只含响应头信息。
     */
    public record HttpHeadResponse(String etag, String lastModified, String contentType, Long contentLength,
                                   String fileName) {
    }
}