package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.Chunk;
import com.bitselect.agent.core.chunk.model.ChunkAssembler;
import com.bitselect.agent.core.chunk.model.ChunkBudget;
import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.parser.model.Block;
import com.bitselect.agent.core.parser.model.HeadingBlock;
import com.bitselect.agent.framework.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】分块调度器：Block 列表 → Chunk 列表的总入口。
 *
 * 【为什么存在】
 * - 需要一个"总指挥"把 Block 列表遍历、分发到对应 chunker、最后装配
 * - 需要一个"查表机制"让新增 Block 类型无需修改调度器代码
 *
 * 【关键设计】
 * - 构造期注册表：遍历所有 BlockChunker 实现，按 blockType 建表
 *   同一类型被两个 chunker 认领时启动即失败（fail-fast）
 * - 流程固定：分发产草稿 → 按节打包（ChunkPacker） → 统一装配（ChunkAssembler）
 *   装配留在末端是因为向量文本要拼章节前缀，而打包只能发生在拼前缀之前
 * - 标题特殊处理：先更新章节路径再照常分发，于是它拿到的是含自己在内的路径，
 *   与其后正文同节而自然同块
 *
 * 【被谁引用】ChunkerNode（分块节点）。
 */
@Component
public class BlockAwareChunkerDispatcher {

    private final HeadingHandler headingHandler;
    private final ChunkPacker chunkPacker;
    private final Map<Class<? extends Block>, BlockChunker<?>> registry;

    public BlockAwareChunkerDispatcher(HeadingHandler headingHandler,
                                       ChunkPacker chunkPacker,
                                       List<BlockChunker<?>> chunkers) {
        this.headingHandler = headingHandler;
        this.chunkPacker = chunkPacker;
        Map<Class<? extends Block>, BlockChunker<?>> table = new HashMap<>();
        for (BlockChunker<?> chunker : chunkers) {
            BlockChunker<?> previous = table.put(chunker.blockType(), chunker);
            if (previous != null) {
                throw new ServiceException(String.format(
                        "Block 分块器注册冲突：类型=%s 同时被 %s 与 %s 认领",
                        chunker.blockType().getSimpleName(),
                        previous.getClass().getSimpleName(), chunker.getClass().getSimpleName()));
            }
        }
        this.registry = Map.copyOf(table);
    }

    /**
     * 【方法用途】把 Block 列表切分为有序块，序号从 0 单调递增。
     */
    public List<Chunk> dispatch(List<Block> blocks, ChunkBudget budget) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }

        HeadingHandler.Outline outline = HeadingHandler.Outline.EMPTY;
        List<ChunkDraft> drafts = new ArrayList<>();
        for (Block block : blocks) {
            if (block instanceof HeadingBlock heading) {
                outline = headingHandler.update(outline, heading);
            }
            drafts.addAll(chunkOne(block, ChunkContext.of(outline.path(), budget)));
        }

        return ChunkAssembler.assembleAll(chunkPacker.pack(drafts, budget));
    }

    @SuppressWarnings("unchecked")
    private List<ChunkDraft> chunkOne(Block block, ChunkContext ctx) {
        BlockChunker<Block> chunker = (BlockChunker<Block>) registry.get(block.getClass());
        if (chunker == null) {
            throw new ServiceException("没有 chunker 认领 Block 类型：" + block.getClass().getName());
        }
        return chunker.chunk(block, ctx);
    }
}