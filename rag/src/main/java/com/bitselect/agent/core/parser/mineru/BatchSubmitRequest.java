package com.bitselect.agent.core.parser.mineru;

/**
 * 【文件用途】MinerU 申请上传链接的请求体（单文件）。
 *
 * 【为什么存在】
 * - MinerU 的"本地文件批量上传解析"链路分两步：先申请上传链接，再上传文件字节
 * - 申请时只提交文件元信息（文件名、是否 OCR、语言等），不带文件内容
 * - 用 record 表达"这次解析请求的完整参数"
 *
 * 【真实请求 JSON】（由 MinerUClient.requestUpload 内部构造）
 * {
 *   "enable_formula": true,
 *   "enable_table":   true,
 *   "language":       "ch",
 *   "files": [
 *     { "name": "xxx.pdf", "is_ocr": false, "data_id": "doc-uuid" }
 *   ]
 * }
 *
 * 【被谁引用】MinerUDocumentParser（构造）、MinerUClient（读取字段）。
 *
 * @param fileName      文件名，必须带正确扩展名，MinerU 靠它识别格式
 * @param dataId        调用方业务标识，从 MinerUStatus 回看
 * @param isOcr         是否强制 OCR
 * @param enableTable   是否提取表格
 * @param enableFormula 是否提取公式
 * @param language      语言代码，如 ch / en / chinese_cht
 */
public record BatchSubmitRequest(
        String fileName,
        String dataId,
        boolean isOcr,
        boolean enableTable,
        boolean enableFormula,
        String language
) {
}