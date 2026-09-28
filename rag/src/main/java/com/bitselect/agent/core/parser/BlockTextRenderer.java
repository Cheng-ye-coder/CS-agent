package com.bitselect.agent.core.parser;

import com.bitselect.agent.core.parser.model.Block;
import com.bitselect.agent.core.parser.model.CodeBlock;
import com.bitselect.agent.core.parser.model.HeadingBlock;
import com.bitselect.agent.core.parser.model.HtmlTableBlock;
import com.bitselect.agent.core.parser.model.ImageBlock;
import com.bitselect.agent.core.parser.model.ListBlock;
import com.bitselect.agent.core.parser.model.ParagraphBlock;
import com.bitselect.agent.core.parser.model.TableBlock;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 【文件用途】Block 列表 → 纯文本渲染器：把解析出的结构化 Block 列表拍平成一大段文本。
 *
 * 【为什么存在】
 * - 整文档模式（ChunkBudget.isWholeDocument()）需要把全部内容作为一个字符串
 * - Pipeline 链路的 ParserNode 需要 rawText（与 CHUNK 链路共用同一份实现）
 * - 与 ChunkerNode 的"保留结构"路径不同：这里是"拍平"路径，不需要保留章节层级
 *
 * 【关键设计】
 * - 简单实现：拼接各 Block 的可读文本表示
 * - 完整 markdown 渲染由各 Chunker 在 BlockAware 路径完成，本类只做基础拍平
 * - ImageBlock 的特殊处理：描述在前、图片 markdown 在后，与 ImageChunker 保持一致——
 *   图生文描述是唯一可检索文本，拍平路径若只渲染 ![](url) 会把描述丢掉，导致永远召回不到
 *
 * 【被谁引用】
 * - ChunkingService（整文档模式）
 * - 未来 Pipeline 链路的 ParserNode
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class BlockTextRenderer {

    /**
     * 【方法用途】把 Block 列表渲染为纯文本。
     *
     * @param blocks 有序 Block 列表，为 null 时返回空串
     * @return 渲染后的纯文本
     */
    public static String render(List<Block> blocks) {
        if (blocks == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            if (b instanceof HeadingBlock h) {
                sb.append("#".repeat(Math.max(1, h.level())))
                        .append(' ').append(h.text() == null ? "" : h.text()).append("\n\n");
            } else if (b instanceof ParagraphBlock p) {
                sb.append(p.text() == null ? "" : p.text()).append("\n\n");
            } else if (b instanceof TableBlock t) {
                if (t.headers() != null) {
                    sb.append(String.join(" | ", t.headers())).append('\n');
                }
                if (t.rows() != null) {
                    for (List<String> row : t.rows()) {
                        sb.append(String.join(" | ", row)).append('\n');
                    }
                }
                sb.append('\n');
            } else if (b instanceof HtmlTableBlock t) {
                sb.append(t.html() == null ? "" : t.html()).append("\n\n");
            } else if (b instanceof ImageBlock i) {
                // 【关键】描述在前、图片 markdown 在后（与 ImageChunker 一致）：
                // 图生文描述是唯一可检索文本，拍平路径若只渲染 ![](url) 会把描述丢掉，导致永远召回不到
                if (i.description() != null && !i.description().isBlank()) {
                    sb.append(i.description().strip()).append("\n\n");
                }
                sb.append("![")
                        .append(i.caption() == null ? "" : i.caption()).append("](")
                        .append(i.asset() == null ? "" : i.asset().publicUrl()).append(")\n\n");
            } else if (b instanceof CodeBlock c) {
                sb.append("```").append(c.language() == null ? "" : c.language())
                        .append('\n').append(c.code() == null ? "" : c.code()).append("\n```\n\n");
            } else if (b instanceof ListBlock l) {
                if (l.items() != null) {
                    for (int idx = 0; idx < l.items().size(); idx++) {
                        sb.append(l.ordered() ? (idx + 1) + ". " : "- ")
                                .append(l.items().get(idx)).append('\n');
                    }
                    sb.append('\n');
                }
            }
        }
        return sb.toString().trim();
    }
}