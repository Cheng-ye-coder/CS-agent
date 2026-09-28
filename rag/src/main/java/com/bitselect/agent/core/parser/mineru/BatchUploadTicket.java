package com.bitselect.agent.core.parser.mineru;

/**
 * 【文件用途】MinerU 申请上传链接接口的返回凭证（单文件）。
 *
 * 【为什么存在】
 * - 申请链接成功后会返回两个关键信息：
 *   1. batchId：后续轮询任务的凭据
 *   2. uploadUrl：MinerU OSS 的预签名 PUT 链接，把文件字节直接 PUT 上去即可
 * - 用 record 让这两个"必须配对使用"的字段不被拆开
 *
 * 【被谁引用】MinerUClient.requestUpload 返回、MinerUDocumentParser 接收。
 *
 * @param batchId   MinerU 分配的 batch_id，轮询/下载凭据
 * @param uploadUrl 文件上传目标 URL，PUT 原始字节，无须鉴权头
 */
public record BatchUploadTicket(
        String batchId,
        String uploadUrl
) {
}