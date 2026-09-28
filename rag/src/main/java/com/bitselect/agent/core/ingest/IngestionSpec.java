package com.bitselect.agent.core.ingest;

import com.bitselect.agent.core.chunk.model.ChunkBudget;
import com.bitselect.agent.core.parser.registry.ParseProfile;

/**
 * 【文件用途】文档级摄取配置：这一篇怎么解析、怎么切。
 *
 * 【为什么存在】
 * - 每篇文档可以有自己的解析档位（FAST / FIDELITY）和分块预算
 * - 对应数据库表 t_knowledge_document 的 ingestion_spec 列（JSONB 存储）
 * - 是解析器选择与分块器执行的输入参数
 *
 * 【关键设计】
 * - 不含 embeddingModel：嵌入模型是知识库级（L2）约束性配置，文档级无权覆盖，
 *   只能由 VectorTarget 提供
 * - version 字段用于未来结构演进时识别旧值
 * - 提供 defaults() / of(profile, budget) 便捷工厂
 *
 * 【被谁引用】
 * - IngestionKernel.run()
 * - IngestionSpecCodec（JSON 序列化/反序列化）
 *
 * @param version      结构版本，用于未来演进时识别旧值
 * @param parseProfile 解析档位
 * @param budget       分块预算
 */
public record IngestionSpec(int version, ParseProfile parseProfile, ChunkBudget budget) {

    public static final int CURRENT_VERSION = 2;

    public IngestionSpec {
        if (version <= 0) {
            throw new IllegalArgumentException("version 必须 > 0，实际 " + version);
        }
        parseProfile = parseProfile == null ? ParseProfile.defaultProfile() : parseProfile;
        budget = budget == null ? ChunkBudget.defaults() : budget;
    }

    /**
     * 【工厂方法】全默认配置：解析档位用 FAST，分块用默认预算。
     */
    public static IngestionSpec defaults() {
        return new IngestionSpec(CURRENT_VERSION, ParseProfile.defaultProfile(), ChunkBudget.defaults());
    }

    /**
     * 【工厂方法】指定档位 + 预算。
     */
    public static IngestionSpec of(ParseProfile parseProfile, ChunkBudget budget) {
        return new IngestionSpec(CURRENT_VERSION, parseProfile, budget);
    }
}