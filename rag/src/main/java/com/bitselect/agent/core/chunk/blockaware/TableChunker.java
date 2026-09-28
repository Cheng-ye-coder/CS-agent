package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.parser.model.TableBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 【文件用途】结构化表格 chunker：按 key-value 渲染长度累加到预算，每块都带完整表头。
 *
 * 【为什么存在】
 * - Excel 解析出的表格需要按行切分，但切碎会丢表头
 * - markdown 表格靠位置对齐列名与值，嵌入模型读不懂位置，需要改成 KV 渲染
 *
 * 【关键设计】
 * - rowsPerChunk 只作硬上限，兼顾宽表不超嵌入上限、窄表不过度碎片化
 * - 展示文本是完整 markdown 表格（前端渲染用）
 * - 向量文本改用"列名: 值"，因为 markdown 表格的位置对齐对嵌入模型无效
 * - 表头不拼进向量文本：KV 正文已逐格自带列名，重复前缀会压缩块间距离
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（dispatch 中调用）。
 */
@Component
public class TableChunker implements BlockChunker<TableBlock> {

    @Override
    public Class<TableBlock> blockType() {
        return TableBlock.class;
    }

    @Override
    public List<ChunkDraft> chunk(TableBlock block, ChunkContext ctx) {
        if (block == null) {
            return List.of();
        }
        List<String> headers = block.headers() == null ? List.of() : block.headers();
        List<List<String>> rows = block.rows() == null ? List.of() : block.rows();
        if (headers.isEmpty() && rows.isEmpty()) {
            return List.of();
        }

        int maxRows = Math.max(1, ctx.budget().rowsPerChunk());
        int budget = rows.size() <= maxRows
                && renderKeyValueRows(headers, rows).length() <= ctx.budget().toleranceChars()
                ? ctx.budget().toleranceChars()
                : Math.max(1, ctx.budget().maxChars());

        List<ChunkDraft> result = new ArrayList<>();

        if (rows.isEmpty()) {
            result.add(buildDraft(block, ctx, headers, List.of()));
            return result;
        }

        List<List<String>> group = new ArrayList<>();
        int groupCost = 0;
        for (List<String> row : rows) {
            int rowCost = renderKeyValueRow(headers, row).length();
            boolean overCap = group.size() >= maxRows;
            boolean overBudget = !group.isEmpty() && groupCost + rowCost > budget;
            if (overCap || overBudget) {
                result.add(buildDraft(block, ctx, headers, group));
                group = new ArrayList<>();
                groupCost = 0;
            }
            group.add(row);
            groupCost += rowCost;
        }
        result.add(buildDraft(block, ctx, headers, group));
        return ChunkDraft.pieces(result);
    }

    private ChunkDraft buildDraft(TableBlock block, ChunkContext ctx, List<String> headers, List<List<String>> rows) {
        ChunkMetadata metadata = ChunkMetadata.builder()
                .outlinePath(ctx.outlinePath())
                .provenance(block.provenance())
                .build();
        return ChunkDraft.of(renderMarkdownTable(headers, rows), renderKeyValueRows(headers, rows), metadata);
    }

    private String renderKeyValueRows(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            String line = renderKeyValueRow(headers, row);
            if (line.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private String renderKeyValueRow(List<String> headers, List<String> row) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < row.size(); c++) {
            String value = row.get(c);
            if (value == null || value.isEmpty()) {
                continue;
            }
            String key = c < headers.size() ? headers.get(c) : "";
            if (!line.isEmpty()) {
                line.append("; ");
            }
            if (!key.isEmpty()) {
                line.append(oneLine(key)).append(": ");
            }
            line.append(oneLine(value));
        }
        return line.toString();
    }

    private static String oneLine(String text) {
        return text.replaceAll("\\r\\n|\\r|\\n", " ");
    }

    private String renderMarkdownTable(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, headers);
        appendSeparator(sb, headers.size());
        for (List<String> row : rows) {
            appendRow(sb, row);
        }
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, List<String> cells) {
        sb.append('|');
        for (String cell : cells) {
            sb.append(' ').append(sanitizeCell(cell)).append(" |");
        }
        sb.append('\n');
    }

    private String sanitizeCell(String cell) {
        if (cell == null || cell.isEmpty()) {
            return "";
        }
        return cell.replace("|", "\\|").replaceAll("\\r\\n|\\r|\\n", "<br>");
    }

    private void appendSeparator(StringBuilder sb, int colCount) {
        sb.append('|');
        sb.append("---|".repeat(Math.max(0, colCount)));
        sb.append('\n');
    }
}