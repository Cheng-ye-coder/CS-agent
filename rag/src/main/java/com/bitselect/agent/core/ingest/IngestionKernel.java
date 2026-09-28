package com.bitselect.agent.core.ingest;

/**
 * 【文件用途】摄取内核接口：定义"解析 → 分块 → 向量化 → 落库"的固定五步骨架。
 *
 * 【为什么存在】
 * - 摄取流程是强顺序的，调用方不应跳过、换序或替换步骤
 * - 提供统一的入口契约，让上层（IngestionNode）只依赖接口
 * - 便于将来替换实现（如 Pipeline 模式 vs 直接模式）
 *
 * 【五步骨架】
 *   ① identity   字节 + 文件名 → MIME
 *   ② parse      (MIME × 档位) → List<Block>
 *   ③ chunk      Block 类型 → chunker + 预算 → List<Chunk>
 *   ④ embed      向量化，此处校验维度
 *   ⑤ index      ChunkSink 扇出，事务边界在此
 *
 * 【职责边界】
 * - 取数是内核之前的事（字节从哪来内核不管）
 * - 任务状态流转与摄取日志归外层
 *
 * 【被谁引用】IngestionNode（入库 Pipeline 的摄取节点）。
 */
public interface IngestionKernel {

    /**
     * 【方法用途】执行一次完整摄取：解析 → 分块 → 向量化 → 落库。
     *
     * @param doc    文档身份，决定资产归属与落库归属
     * @param bytes  文件字节
     * @param spec   文档级配置：解析档位 + 分块预算
     * @param target 向量落点：逻辑分区 + 嵌入模型 + 维度
     * @return 摄取结果
     */
    IngestionOutcome run(DocumentRef doc,
                         byte[] bytes,
                         IngestionSpec spec,
                         VectorTarget target);
}