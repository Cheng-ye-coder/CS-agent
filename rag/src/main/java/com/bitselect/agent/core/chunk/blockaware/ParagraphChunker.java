package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.chunk.text.TextSplitter;
import com.bitselect.agent.core.parser.model.ParagraphBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 【文件用途】段落 chunker：优先整段保留，超出容忍上限才按块大小降级切分。
 *
 * 【为什么存在】
 * - 段落是最常见的 Block 类型，需要按 token 精细切分
 * - 切分逻辑复杂（边界回溯、文本归一化），委托给 TextSplitter
 *
 * 【关键设计】
 * - 先按容忍上限量一次，切不动说明整段撑得住；量出多片才退回块大小重切
 * - 切分一律委托 TextSplitter，由它做边界回溯（换行 / 中文句末 / 英文句末）
 *   与文本归一化（URL 断行修复、CJK 软换行合并），本类不自行按下标截断
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（dispatch 中调用）。
 */
@Component
public class ParagraphChunker implements BlockChunker<ParagraphBlock> {

    @Override
    public Class<ParagraphBlock> blockType() {
        return ParagraphBlock.class;
    }

    @Override
    public List<ChunkDraft> chunk(ParagraphBlock block, ChunkContext ctx) {
        if (block == null) {
            return List.of();
        }
        int overlap = ctx.budget().overlapChars();
        List<String> pieces = TextSplitter.split(block.text(), ctx.budget().toleranceChars(), overlap);
        if (pieces.size() > 1) {
            pieces = TextSplitter.split(block.text(), ctx.budget().maxChars(), overlap);
        }
        if (pieces.isEmpty()) {
            return List.of();
        }

        ChunkMetadata metadata = ChunkMetadata.builder()
                .outlinePath(ctx.outlinePath())
                .provenance(block.provenance())
                .build();

        List<ChunkDraft> result = new ArrayList<>(pieces.size());
        for (String piece : pieces) {
            result.add(ChunkDraft.of(piece, metadata));
        }
        return ChunkDraft.pieces(result);
    }
}