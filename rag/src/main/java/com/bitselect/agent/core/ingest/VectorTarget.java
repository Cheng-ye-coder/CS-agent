package com.bitselect.agent.core.ingest;

/**
 * 【文件用途】向量落点身份：块写到哪个逻辑分区、用哪个模型、必须是多少维。
 *
 * 【为什么存在】
 * - 向量化需要知道"用哪个模型、输出多少维"，否则无法调用 EmbeddingService
 * - 维度是部署级硬约束（写在建表语句里），必须随身携带校验
 * - 分区键让同一个部署支持多个知识库（不同知识库用不同 collection）
 *
 * 【关键设计】
 * - partition 是逻辑分区键，取自知识库的 collection_name
 *   与 rag.core.vector.VectorSpaceId 表示的物理空间（PG 共享表 / Milvus collection）不是一回事
 * - 模型与维度随身携带，缺一个都不允许落到系统默认值——嵌入模型是知识库级约束性配置
 *
 * 【被谁引用】
 * - IngestionKernel.run()
 * - ChunkEmbeddingService（校验维度）
 * - ChunkSink / ChunkIndexWriter（写入分区）
 *
 * @param partition      逻辑分区键，取自知识库的 collection_name
 * @param embeddingModel 嵌入模型 ID，取自知识库配置
 * @param dimension      向量维度，取自部署级配置，全局硬约束
 */
public record VectorTarget(String partition, String embeddingModel, int dimension) {

    public VectorTarget {
        if (partition == null || partition.isBlank()) {
            throw new IllegalArgumentException("partition 不能为空");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel 不能为空，partition=" + partition
                    + "——嵌入模型是知识库级约束性配置，不允许回落到系统默认");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension 必须 > 0，实际 " + dimension);
        }
    }
}