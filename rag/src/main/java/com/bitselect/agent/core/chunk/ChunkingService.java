package com.bitselect.agent.core.chunk;

import com.bitselect.agent.core.chunk.blockaware.BlockAwareChunkerDispatcher;
import com.bitselect.agent.core.chunk.model.Chunk;
import com.bitselect.agent.core.chunk.model.ChunkAssembler;
import com.bitselect.agent.core.chunk.model.ChunkBudget;
import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.parser.BlockTextRenderer;
import com.bitselect.agent.core.parser.model.Block;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 【文件用途】分块入口：解析产出的 Block 列表 → 成品块列表。
 *
 * 【为什么存在】
 * - 上游（IngestionNode）不知道该怎么切分，只需要调用 chunk(blocks, budget) 一行
 * - 分块有两种模式，需要统一入口判断：
 *     1. 整文档单块（budget.isWholeDocument()）
 *     2. 按 Block 类型分发（BlockAwareChunkerDispatcher）
 * - 只有两个分支，分支依据是预算而不是用户选的策略，逻辑简单清晰
 *
 * 【关键设计】
 * - 整文档模式：把全部 Block 渲染成一个大字符串，作为单块返回
 * - 分发模式：委托 BlockAwareChunkerDispatcher 按类型逐个处理
 * - 渲染整文档用 BlockTextRenderer（与各 chunker 走不同路径，因为不需要保留结构）
 *
 * 【被谁引用】IngestionNode（入库 Pipeline 的分块节点）。
 */
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final BlockAwareChunkerDispatcher blockAwareChunkerDispatcher;

    /**
     * 【方法用途】切分为块列表，序号从 0 单调递增，无可切内容时返回空列表。
     *
     * @param blocks Block 列表（解析器产出）
     * @param budget 分块预算，整文档模式由 ChunkBudget.isWholeDocument() 表达
     * @return 成品块列表
     */
    public List<Chunk> chunk(List<Block> blocks, ChunkBudget budget) {
        if (budget.isWholeDocument()) {
            return wholeDocument(blocks);
        }
        return blockAwareChunkerDispatcher.dispatch(blocks, budget);
    }

    /**
     * 【方法用途】整文档单块：把全部 Block 渲染成一个大字符串，作为单块返回。
     *
     * 用于"用户明确不想切分"的场景（如短文档、代码文件），此时不需要保留结构，
     * 直接把所有内容拼成一段即可。
     */
    private List<Chunk> wholeDocument(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        String whole = BlockTextRenderer.render(blocks);
        if (!StringUtils.hasText(whole)) {
            return List.of();
        }
        ChunkMetadata metadata = ChunkMetadata.builder()
                .provenance(blocks.get(0).provenance())
                .build();
        return List.of(ChunkAssembler.assemble(0, ChunkDraft.of(whole, metadata)));
    }
}