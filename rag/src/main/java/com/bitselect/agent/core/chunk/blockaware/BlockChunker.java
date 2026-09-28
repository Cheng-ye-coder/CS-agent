package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.parser.model.Block;

import java.util.List;

/**
 * 【文件用途】Block 类型专属的切分器接口：每个实现自报"处理哪个 Block 类型"、"怎么切"。
 *
 * 【为什么存在】
 * - 不同 Block 的切分规则差异极大（表格按行、代码不切、段落按 token）
 * - 用接口 + 泛型让调度器靠查表工作，新增 Block 类型只补一个实现即可
 * - 能否与邻居并块不由 Block 类型决定，由 ChunkPacker 按预算算出来，职责分离
 *
 * 【被谁引用】
 * - BlockAwareChunkerDispatcher（启动时收集所有实现）
 * - 各具体 Chunker（实现本接口）
 *
 * @param <B> 该 chunker 处理的 Block 子类型
 */
public interface BlockChunker<B extends Block> {

    /**
     * 【方法用途】注册键：本 chunker 处理的 Block 类型，供调度器建表。
     */
    Class<B> blockType();

    /**
     * 【方法用途】把单个 Block 切分为若干草稿，可能为空。
     *
     * 序号与块 ID 由装配阶段统一分配。
     * 切与不切的判据全类型统一：整块撑得住 ChunkBudget.toleranceChars() 就不切，
     * 超出才按块大小降级切分，且切点一律落在结构边界（行、表格行、列表项、句末）上。
     */
    List<ChunkDraft> chunk(B block, ChunkContext ctx);
}