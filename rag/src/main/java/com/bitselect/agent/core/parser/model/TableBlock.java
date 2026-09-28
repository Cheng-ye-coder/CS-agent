package com.bitselect.agent.core.parser.model;

import java.util.List;

/**
 * 【文件用途】结构化表格 Block：已经被解析成 headers + rows 的二维表。
 *
 * 【为什么存在】
 * - Excel 解析器能直接输出结构化的行列数据，不需要走 HTML
 * - 按行切分时，每块都要重复表头（否则表格切碎后失去列名）
 *
 * 【分块策略】
 * - 由 TableChunker 按 rowsPerChunk 切分
 * - 每个 chunk 都重复带上 headers，保证每块可独立理解
 *
 * 【前置处理】
 * - 到这里合并单元格已被 ExcelTableNormalizer 展开填充
 * - 多行表头已展平为单行，列名以竖线拼接如 "财务|收入"
 *
 * @param provenance 来源信息
 * @param headers    表头列名（已展平）
 * @param rows       数据行，每行是列值的 List
 */
public record TableBlock(
        Provenance provenance,
        List<String> headers,
        List<List<String>> rows
) implements Block {
}