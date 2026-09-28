package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkBudget;

import java.util.List;

/**
 * 【文件用途】切分上下文：调度器遍历 Block 列表时构造并传给每个 chunker。
 *
 * 【为什么存在】
 * - Chunker 需要"当前章节路径"和"分块预算"两个上下文，不能作为参数逐一传递
 * - 封装成对象后，未来加字段（如解析器类型）不破接口
 *
 * 【关键设计】
 * - 不可变 record，构造时对 outlinePath 做防御性拷贝
 * - 章节路径由 HeadingHandler 累积，调度器负责传递
 *
 * @param outlinePath 当前累积的章节路径（如 ["第一章", "1.1 概述"]）
 * @param budget      分块预算
 */
public record ChunkContext(List<String> outlinePath, ChunkBudget budget) {

    public ChunkContext {
        outlinePath = outlinePath == null ? List.of() : List.copyOf(outlinePath);
    }

    public static ChunkContext of(List<String> outlinePath, ChunkBudget budget) {
        return new ChunkContext(outlinePath, budget);
    }
}