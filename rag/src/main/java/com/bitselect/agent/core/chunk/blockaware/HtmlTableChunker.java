package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.parser.model.HtmlTableBlock;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【文件用途】HTML 表格 chunker：按 tr 边界切分，每块重复表头行并包回完整 table。
 *
 * 【为什么存在】
 * - MinerU 等解析器输出的是原始 HTML 表格，不是结构化的 headers + rows
 * - 不转成管道表：合并单元格与单元格内的换行在展开成二维表时会失真
 * - 展示与检索都用同一份 HTML，保持完整
 *
 * 【关键设计】
 * - 取原始 table 开标签而非写死：作者写在上面的 border / class 等属性得跟着每一块走
 * - 过滤值为 1 的 colspan / rowspan（MinerU 逐格都写，一张十来行的表能被撑到三倍）
 * - 整张表撑得住容忍上限就不切
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（dispatch 中调用）。
 */
@Component
public class HtmlTableChunker implements BlockChunker<HtmlTableBlock> {

    private static final Pattern ROW = Pattern.compile("<tr\\b[^>]*>.*?</tr>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern NO_OP_SPAN = Pattern.compile("\\s+(?:colspan|rowspan)\\s*=\\s*[\"']?1[\"']?",
            Pattern.CASE_INSENSITIVE);

    private static final String TABLE_CLOSE = "</table>";

    @Override
    public Class<HtmlTableBlock> blockType() {
        return HtmlTableBlock.class;
    }

    @Override
    public List<ChunkDraft> chunk(HtmlTableBlock block, ChunkContext ctx) {
        if (block == null || !StringUtils.hasText(block.html())) {
            return List.of();
        }
        ChunkMetadata metadata = ChunkMetadata.builder()
                .outlinePath(ctx.outlinePath())
                .provenance(block.provenance())
                .build();

        String html = NO_OP_SPAN.matcher(block.html()).replaceAll("");
        List<String> rows = splitRows(html);
        if (rows.size() < 2) {
            return List.of(ChunkDraft.of(html, metadata));
        }

        String open = openTag(html);
        String header = rows.get(0);
        int maxRows = Math.max(1, ctx.budget().rowsPerChunk());
        int budget = rows.size() - 1 <= maxRows && html.length() <= ctx.budget().toleranceChars()
                ? ctx.budget().toleranceChars()
                : Math.max(1, ctx.budget().maxChars());
        int overhead = open.length() + TABLE_CLOSE.length() + header.length();

        List<ChunkDraft> result = new ArrayList<>();
        List<String> group = new ArrayList<>();
        int groupLen = 0;
        for (String row : rows.subList(1, rows.size())) {
            boolean overCap = group.size() >= maxRows;
            boolean overBudget = !group.isEmpty() && overhead + groupLen + row.length() > budget;
            if (overCap || overBudget) {
                result.add(ChunkDraft.of(render(open, header, group), metadata));
                group = new ArrayList<>();
                groupLen = 0;
            }
            group.add(row);
            groupLen += row.length();
        }
        result.add(ChunkDraft.of(render(open, header, group), metadata));
        return ChunkDraft.pieces(result);
    }

    private static List<String> splitRows(String html) {
        List<String> rows = new ArrayList<>();
        Matcher matcher = ROW.matcher(html);
        while (matcher.find()) {
            rows.add(matcher.group());
        }
        return rows;
    }

    private static String openTag(String html) {
        int end = html.indexOf('>');
        return end < 0 ? "<table>" : html.substring(0, end + 1);
    }

    private static String render(String open, String header, List<String> rows) {
        StringBuilder sb = new StringBuilder(open).append(header);
        for (String row : rows) {
            sb.append(row);
        }
        return sb.append(TABLE_CLOSE).toString();
    }
}