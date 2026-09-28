package com.bitselect.agent.core.parser.model;

import java.util.List;
import java.util.Map;

/**
 * 【文件用途】解析器统一输出：有序 Block 列表 + 文档级元数据。
 *
 * 【为什么存在】
 * - 解析器处理完文档后，需要一个"容器"把结果返回给下游
 * - 不能只返回 List<Block>：文档级的元数据（解析器名、页数、耗时）会丢失
 * - 排障时想知道"这篇文档是用 Tika 还是 MinerU 解析的"
 *
 * 【作为什么契约】
 * - 解析阶段 → 分块阶段之间的唯一契约
 * - DocumentParser.parseStructured() 的返回值
 *
 * 【被谁引用】ChunkerNode（分块器入口）。
 *
 * @param blocks   有序 Block 列表（章节、段落、表格、图片等按文档原始顺序）
 * @param metadata 文档级元数据，如来源、页数、解析器、耗时等
 */
public record ParsedDocument(List<Block> blocks, Map<String, Object> metadata) {

    /**
     * 便捷构造：只关心 blocks 时用此方法，metadata 置空 Map。
     */
    public static ParsedDocument of(List<Block> blocks) {
        return new ParsedDocument(blocks != null ? blocks : List.of(), Map.of());
    }

    /**
     * 完整构造：blocks 和 metadata 都提供。
     */
    public static ParsedDocument of(List<Block> blocks, Map<String, Object> metadata) {
        return new ParsedDocument(blocks != null ? blocks : List.of(), metadata != null ? metadata : Map.of());
    }
}