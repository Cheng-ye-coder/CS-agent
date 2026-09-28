package com.bitselect.agent.rag.service;

import com.bitselect.agent.rag.dto.StoredFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 【文件用途】文件存储服务：后端无关的高层门面，屏蔽 S3 / OSS 差异。
 *
 * 【为什么存在】
 * - 知识库文档与多模态资产需要落对象存储，但业务代码不该关心底层是 S3 还是 OSS
 * - 所有知识库文档共用一个全局桶，按 namespace（= collectionName）划分目录
 * - 多模态资产落公共读资产桶，供浏览器匿名预览
 * - 存储引用只保留裸 key（如 {namespace}/{uuid}.ext）：桶是部署级配置常量、不写进数据
 *
 * 【被谁引用】
 * - knowledge/handler/RemoteFileFetcher（远程文件拉取后上传）
 * - core/parser/image/ImageDocumentParser（图片上传）
 * - core/parser/mineru/MinerUResultUnpacker（MinerU 内嵌图上传）
 */
public interface FileStorageService {

    /**
     * 【方法用途】上传知识库文档（MultipartFile 版本，流式低内存）。
     *
     * @param namespace 知识库命名空间（collectionName）
     */
    StoredFileDTO upload(String namespace, MultipartFile file);

    /**
     * 【方法用途】上传知识库文档（InputStream 版本）。
     */
    StoredFileDTO upload(String namespace, InputStream content, long size, String originalFilename, String contentType);

    /**
     * 【方法用途】上传知识库文档（byte[] 版本）。
     */
    StoredFileDTO upload(String namespace, byte[] content, String originalFilename, String contentType);

    /**
     * 【方法用途】上传知识库文档（SDK 原生，带自动重试）。
     *
     * 代价是可能将 payload 缓冲到堆内存；适用于小文件或对可靠性敏感的场景。
     */
    StoredFileDTO reliableUpload(String namespace, InputStream content, long size, String originalFilename, String contentType);

    /**
     * 【方法用途】上传多模态资产（公共读桶）。
     */
    StoredFileDTO uploadAsset(byte[] content, String originalFilename, String contentType);

    /**
     * 【方法用途】打开我方文档的输入流（落知识库桶）。
     */
    InputStream openStream(String key);

    /**
     * 【方法用途】删除我方文档（落知识库桶）。
     */
    void deleteByUrl(String key);

    /**
     * 【方法用途】把我方资产裸 key 转为浏览器可匿名直连的公开预览 URL。
     *
     * 要求资产桶已开公共读，仅用于多模态资产等可公开预览的对象。
     */
    String getPublicUrl(String key);

    /**
     * 【方法用途】创建知识库空间（幂等）：在全局知识库桶下建立目录标记。
     */
    void createKnowledgeSpace(String namespace);

    /**
     * 【方法用途】删除知识库空间（幂等）：清空该 namespace 前缀的所有对象。
     */
    void deleteKnowledgeSpace(String namespace);
}