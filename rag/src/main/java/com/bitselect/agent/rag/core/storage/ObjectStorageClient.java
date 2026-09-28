package com.bitselect.agent.rag.core.storage;

import java.io.InputStream;

/**
 * 【文件用途】对象存储底层 SPI：只认 (bucket, key) 裸操作。
 *
 * 【为什么存在】
 * - S3 兼容存储（rustfs / minio）与阿里云 OSS 各一实现，由 rag.storage.type 二选一注册
 * - 上层 DefaultFileStorageService 只依赖这个接口，不感知具体后端
 * - namespace/key 组装、桶归属、类型探测等后端无关逻辑收敛在上层，本接口只负责与具体存储对话
 *
 * 【两个上传方法】
 * - streamPut：低内存流式上传，不保证重试（适合大文件）
 * - reliablePut：SDK 原生带自动重试（适合小文件或可靠性敏感场景）
 *
 * 【被谁引用】
 * - DefaultFileStorageService（文件存储门面）
 */
public interface ObjectStorageClient {

    /**
     * 【方法用途】流式上传（低内存）。
     *
     * 各后端选择自身最省内存的方式实现：S3 用预签名 URL + HttpURLConnection 零堆流式，
     * OSS 用 SDK putObject 配合 contentLength 按块流式。不保证自动重试。
     */
    void streamPut(String bucket, String key, InputStream content, long size, String contentType);

    /**
     * 【方法用途】可靠上传（SDK 原生，带自动重试）。
     *
     * 网络抖动 / 超时自动重发，代价是可能将 payload 缓冲到堆内存。
     */
    void reliablePut(String bucket, String key, InputStream content, long size, String contentType);

    /**
     * 【方法用途】打开对象读取流，调用方负责关闭。
     */
    InputStream getObject(String bucket, String key);

    /**
     * 【方法用途】删除单个对象（幂等）。
     */
    void deleteObject(String bucket, String key);

    /**
     * 【方法用途】按前缀分页列举并批量删除（幂等），用于删除知识库目录。
     */
    void deleteByPrefix(String bucket, String prefix);

    /**
     * 【方法用途】判断对象是否存在。
     */
    boolean objectExists(String bucket, String key);

    /**
     * 【方法用途】判断桶是否存在。
     */
    boolean bucketExists(String bucket);

    /**
     * 【方法用途】创建桶（幂等：已存在视为成功）。
     */
    void createBucket(String bucket);

    /**
     * 【方法用途】给桶下发公共读策略（幂等），使桶内对象可被浏览器匿名直连预览。
     */
    void setBucketPublicRead(String bucket);

    /**
     * 【方法用途】拼装浏览器可直连的公开 URL。
     *
     * S3 / rustfs 为 path-style（{base}/{bucket}/{key}），OSS 为虚拟主机式（{bucketBase}/{key}）。
     */
    String buildPublicUrl(String bucket, String key);
}