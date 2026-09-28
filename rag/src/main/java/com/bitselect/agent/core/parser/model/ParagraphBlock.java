package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】段落 Block：文档中一段连续的普通文本。
 *
 * 【为什么存在】
 * - 段落是文档里最常见的单元，需要一个类型承载
 * - 段落的切分策略最灵活（可跨段合并到目标长度，但不跨 heading）
 *
 * 【切分规则】
 * - 由 ParagraphChunker 按 token 切分
 * - 可跨段合并到目标块大小（提高检索质量）
 * - 但不能跨 HeadingBlock 边界（会破坏章节完整性）
 *
 * 【关键设计】
 * - 注释明确"不能假定它是纯文本"——markdown 里可能内嵌 HTML
 * - 内嵌的非表格 HTML 原样落在此处，表格另走 HtmlTableBlock
 *
 * @param provenance 来源信息
 * @param text       段落文本，保留链接、图片与行内代码标记，丢掉强调标记（**bold** → bold）
 */
public record ParagraphBlock(
        Provenance provenance,
        String text
) implements Block {
}