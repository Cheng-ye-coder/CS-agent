package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】原始 HTML 表格 Block：保留完整 <table> 标签的表格。
 *
 * 【为什么存在】
 * - MinerU 等版面解析器输出的表格是 HTML 格式，不是结构化的行列数据
 * - 合并单元格、单元格内换行、公式片段在展开成二维表时【必然失真】
 * - 保留原 HTML 让前端自行渲染，能完整还原表格样式
 *
 * 【与 TableBlock 的区别】
 * - TableBlock：已经是结构化的 headers + rows，Excel 解析器产出
 * - HtmlTableBlock：原始 HTML 字符串，展示端负责渲染
 *
 * 【分块策略】由 HtmlTableChunker 按行切分。
 *
 * @param provenance 来源信息
 * @param html       完整表格 HTML，以 <table 开头
 */
public record HtmlTableBlock(
        Provenance provenance,
        String html
) implements Block {
}