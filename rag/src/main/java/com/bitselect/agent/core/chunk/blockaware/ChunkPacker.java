package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkBudget;
import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.parser.model.AssetRef;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 【文件用途】块打包器：把相邻的小草稿按体量合并成"合规大小"的块。
 *
 * 【为什么存在】
 * - 各 Chunker 切出的草稿粒度太细（一个段落一块、一个表格行一块），直接向量化会导致 topK 名额被碎片占满
 * - 需要按"节边界"（相邻两个标题之间）合并，同时保证块大小在合理范围
 *
 * 【核心规则】
 * - 切口只落在"节边界"上，故不做块级重叠：重叠是为了防答案被切断，而节边界处没有被切断的句子
 * - 唯一例外：整节超出容忍上限时的节内切分，那一层的重叠由 TextSplitter 在切段落时负责
 * - 一节整体不超容忍上限就是原子的，要么整节并进当前块要么整节自己成块
 *
 * 【关键阈值】
 * - maxChars：目标块大小
 * - minChars（= maxChars/4）：最小块体量，低于它的余量并回上一块
 * - toleranceChars：容忍上限，超出必须切
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（dispatch 中调用）。
 */
@Component
public class ChunkPacker {

    private static final String SEPARATOR = "\n\n";

    /**
     * 最小块体量取块大小的几分之一。
     */
    private static final int MIN_CHARS_DIVISOR = 4;

    /**
     * 【方法用途】打包成块。
     */
    public List<ChunkDraft> pack(List<ChunkDraft> drafts, ChunkBudget budget) {
        if (drafts == null || drafts.size() <= 1) {
            return drafts == null ? List.of() : drafts;
        }

        int maxChars = budget.maxChars();
        int minChars = Math.max(1, maxChars / MIN_CHARS_DIVISOR);
        List<ChunkDraft> result = new ArrayList<>();
        List<ChunkDraft> buffer = new ArrayList<>();

        for (List<ChunkDraft> section : splitSections(drafts)) {
            int sectionLen = totalLength(section);
            if (sectionLen > budget.toleranceChars()) {
                buffer = packWithin(buffer, section, budget, minChars, result);
                continue;
            }
            if (!buffer.isEmpty() && breakBefore(totalLength(buffer), sectionLen, minChars, budget)) {
                flush(buffer, result, budget, minChars);
                buffer = new ArrayList<>();
            }
            buffer.addAll(section);
            if (totalLength(buffer) > maxChars) {
                flush(buffer, result, budget, minChars);
                buffer = new ArrayList<>();
            }
        }
        flush(buffer, result, budget, minChars);
        return result;
    }

    /**
     * 【方法用途】判断到了下一节的边界要不要断开。
     *
     * minChars 管下限、maxChars 管目标，两条职责不共用一个阈值：
     * 让"攒够 minChars 就断"兼任断点判据，等于把下限变成事实上的目标——
     * 标题密集的文档配 1024 也只切得出 300 上下的块。
     */
    private static boolean breakBefore(int bufferLen, int sectionLen, int minChars, ChunkBudget budget) {
        if (bufferLen < minChars) {
            return bufferLen + SEPARATOR.length() + sectionLen > budget.toleranceChars();
        }
        return bufferLen + SEPARATOR.length() + sectionLen > budget.maxChars();
    }

    /**
     * 【方法用途】按标题切节：标题起一节，标题之前的散块自成一节。
     */
    private static List<List<ChunkDraft>> splitSections(List<ChunkDraft> drafts) {
        List<List<ChunkDraft>> sections = new ArrayList<>();
        List<ChunkDraft> current = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            if (draft.heading() && !current.isEmpty()) {
                sections.add(current);
                current = new ArrayList<>();
            }
            current.add(draft);
        }
        if (!current.isEmpty()) {
            sections.add(current);
        }
        return sections;
    }

    /**
     * 【方法用途】节内切分：整节撑破容忍上限时逐草稿贪心累加。
     *
     * 残留交回上层而不就地落块，让它有机会与下一节合并——否则一节的尾巴总是单独成块。
     */
    private static List<ChunkDraft> packWithin(List<ChunkDraft> carried, List<ChunkDraft> section,
                                               ChunkBudget budget, int minChars, List<ChunkDraft> result) {
        int maxChars = budget.maxChars();
        List<ChunkDraft> buffer = new ArrayList<>(carried);
        for (ChunkDraft draft : section) {
            int addLen = contentLength(draft);
            if (draft.piece() || addLen >= maxChars) {
                List<ChunkDraft> leadIn = pollLeadIn(buffer, draft, budget);
                flush(buffer, result, budget, minChars);
                List<ChunkDraft> parts = new ArrayList<>(leadIn);
                parts.add(draft);
                result.add(parts.size() == 1 ? draft : merge(parts));
                buffer = new ArrayList<>();
                continue;
            }
            if (!buffer.isEmpty() && totalLength(buffer) + SEPARATOR.length() + addLen > maxChars) {
                flush(buffer, result, budget, minChars);
                buffer = new ArrayList<>();
            }
            buffer.add(draft);
        }
        return buffer;
    }

    /**
     * 【方法用途】取出可并入大块的前导语，取到的草稿已从缓冲区移除。
     *
     * 表格的"保证金单位为元"、代码块的用途说明都写在前一段里，甩成孤块等于把检索入口与内容拆开。
     */
    private static List<ChunkDraft> pollLeadIn(List<ChunkDraft> buffer, ChunkDraft target, ChunkBudget budget) {
        int limit = budget.maxChars();
        int taken = 0;
        int from = buffer.size();
        while (from > 0) {
            int next = taken + SEPARATOR.length() + contentLength(buffer.get(from - 1));
            if (next > limit) {
                break;
            }
            taken = next;
            from--;
        }
        if (from == buffer.size() || contentLength(target) + taken > budget.toleranceChars()) {
            return List.of();
        }
        List<ChunkDraft> leadIn = new ArrayList<>(buffer.subList(from, buffer.size()));
        buffer.subList(from, buffer.size()).clear();
        return leadIn;
    }

    /**
     * 【方法用途】缓冲区落块，不足最小块体量的余量并回上一块。
     *
     * 这是"不产出小于 minChars 的块"的兜底：文档结尾、以及一节撑破容忍上限后剩下的尾巴，
     * 都可能是二十来字的碎屑，单独成块既召不回也白占一个 topK 名额，并回去哪怕跨了节也划算。
     */
    private static void flush(List<ChunkDraft> buffer, List<ChunkDraft> result,
                              ChunkBudget budget, int minChars) {
        if (buffer.isEmpty()) {
            return;
        }
        ChunkDraft packed = buffer.size() == 1 ? buffer.get(0) : merge(buffer);
        if (!result.isEmpty() && contentLength(packed) < minChars) {
            ChunkDraft previous = result.get(result.size() - 1);
            if (contentLength(previous) + SEPARATOR.length() + contentLength(packed) <= budget.toleranceChars()) {
                result.set(result.size() - 1, merge(List.of(previous, packed)));
                return;
            }
        }
        result.add(packed);
    }

    /**
     * 【方法用途】合并多块。
     *
     * 展示文本与检索正文分别拼接，资产取并集，章节路径取各块的公共前缀。
     * 检索正文按"显式值优先、否则回落展示文本"逐块拼接：图片块的检索正文特意去掉了 URL 噪声，
     * 一律取展示文本会让向量退化成带 URL 的文本；路径取公共前缀而非其中某一块的，
     * 前导语可能来自上一节，取大块那份等于把上一节的内容记到本节名下。
     */
    private static ChunkDraft merge(List<ChunkDraft> parts) {
        StringBuilder content = new StringBuilder();
        StringBuilder body = new StringBuilder();
        boolean hasExplicitBody = false;
        boolean heading = false;
        List<AssetRef> assets = new ArrayList<>();

        for (ChunkDraft draft : parts) {
            appendPart(content, draft.content());
            appendPart(body, draft.effectiveBody());
            hasExplicitBody |= draft.hasExplicitBody();
            heading |= draft.heading();
            assets.addAll(draft.metadata().assets());
        }

        ChunkMetadata merged = ChunkMetadata.builder()
                .outlinePath(parts.get(0).metadata().outlinePath().subList(0, commonPrefixLength(parts)))
                .assets(assets)
                .provenance(parts.get(0).metadata().provenance())
                .build();
        return new ChunkDraft(content.toString(), hasExplicitBody ? body.toString() : null,
                merged, false, heading);
    }

    private static int commonPrefixLength(List<ChunkDraft> drafts) {
        List<String> first = drafts.get(0).metadata().outlinePath();
        int common = first.size();
        for (ChunkDraft draft : drafts) {
            common = Math.min(common, commonPrefixLength(first, draft.metadata().outlinePath()));
        }
        return common;
    }

    private static int commonPrefixLength(List<String> a, List<String> b) {
        int limit = Math.min(a.size(), b.size());
        int i = 0;
        while (i < limit && Objects.equals(a.get(i), b.get(i))) {
            i++;
        }
        return i;
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (!StringUtils.hasText(part)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(SEPARATOR);
        }
        sb.append(part);
    }

    private static int totalLength(List<ChunkDraft> drafts) {
        int len = 0;
        for (int i = 0; i < drafts.size(); i++) {
            len += (i == 0 ? 0 : SEPARATOR.length()) + contentLength(drafts.get(i));
        }
        return len;
    }

    private static int contentLength(ChunkDraft draft) {
        return StringUtils.hasText(draft.content()) ? draft.content().length() : 0;
    }
}