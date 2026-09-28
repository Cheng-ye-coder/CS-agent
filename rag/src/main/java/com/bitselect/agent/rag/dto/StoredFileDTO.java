package com.bitselect.agent.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】已存储文件的元数据：文件上传到对象存储后返回的信息。
 *
 * 【为什么存在】
 * - 文件上传后需要把"存储地址 + 类型 + 大小"回传给调用方
 * - 用于知识库文档记录、图片资源引用等场景
 * - 是 RemoteFileFetcher / ImageDocumentParser / MinerUResultUnpacker 的共同产物类型
 *
 * 【关键字段】
 * - url：对象存储中的路径（相对路径，非公网 URL）
 * - detectedType：MIME 探测得出的展示类型（如 "pdf" / "docx"）
 * - mimeType：真实 MIME（如 "application/pdf"）
 * - size：文件字节数
 * - originalFilename：原始文件名
 *
 * 【被谁引用】
 * - knowledge/handler/RemoteFileFetcher（远程文件拉取后返回）
 * - core/parser/image/ImageDocumentParser（图片上传后返回）
 * - core/parser/mineru/MinerUResultUnpacker（MinerU 内嵌图上传后返回）
 * - rag/service/FileStorageService（上传接口的返回值）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoredFileDTO {

    /**
     * 对象存储中的路径（相对路径，非公网 URL）
     */
    private String url;

    /**
     * MIME 探测得出的展示类型（如 "pdf" / "docx"）
     */
    private String detectedType;

    /**
     * 真实 MIME（如 "application/pdf"）
     */
    private String mimeType;

    /**
     * 文件字节数
     */
    private Long size;

    /**
     * 原始文件名
     */
    private String originalFilename;
}