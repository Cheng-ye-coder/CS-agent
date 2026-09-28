package com.bitselect.agent.core.ingest;

import com.bitselect.agent.core.chunk.model.Chunk;

import java.util.List;

/**
 * 【文件用途】摄取结果：一次完整摄取任务产出的统计信息与块列表。
 *
 * 【为什么存在】
 * - 外层（IngestionNode）需要知道"摄取了多少块、用了哪个解析器、各阶段耗时"
 * - 用于写摄取日志（t_knowledge_document_chunk_log 表）
 * - 用于更新文档统计（chunk_count）
 *
 * 【关键设计】
 * - 只到 Chunk 为止，向量已由内核写进各索引后端，不再随结果传出一份
 * - 提供 chunkCount() 便捷方法
 * - timings 记录四阶段耗时（解析 / 分块 / 向量化 / 落库）
 *
 * 【被谁引用】
 * - IngestionKernel.run() 返回
 * - IngestionNode（读取结果写日志）
 *
 * @param mimeType   识别出的真实 MIME
 * @param parserType 实际命中的解析器类型
 * @param blockCount 解析产出的 Block 数量
 * @param chunks     最终落库的块
 * @param timings    各阶段耗时
 */
public record IngestionOutcome(
        String mimeType,
        String parserType,
        int blockCount,
        List<Chunk> chunks,
        IngestionTimings timings
) {

    public IngestionOutcome {
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
        timings = timings == null ? IngestionTimings.zero() : timings;
    }

    public int chunkCount() {
        return chunks.size();
    }

    /**
     * 【内部结构】各阶段耗时（毫秒）。
     *
     * 解析含类型识别，分块含 Block / Chunk 两层插槽加工。
     */
    public record IngestionTimings(long parseMillis, long chunkMillis, long embedMillis, long indexMillis) {

        public static IngestionTimings zero() {
            return new IngestionTimings(0, 0, 0, 0);
        }
    }
}