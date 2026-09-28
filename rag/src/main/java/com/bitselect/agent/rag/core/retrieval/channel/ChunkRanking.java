package com.bitselect.agent.rag.core.retrieval.channel;

import com.bitselect.agent.framework.convention.RetrievedChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * 【文件用途】通道出口的名次整理：把主路与补充路按分数降序合并。
 *
 * 【为什么存在】
 * - "通道出口按相关性有序"是下游 RRF 按名次取分依赖的不变式
 * - 三条 KB 通道共用这一份实现，规则改动只落一处
 *
 * 【关键设计】
 * - 不能主路在前补充路拼在后：两路分数同源，拼接序会让补充路的强命中恒排在主路弱命中之后
 * - 补充路为空时主路同样重排，后端返回乱序（如 PG relaxed_order）也被兜住
 *
 * 【被谁引用】VectorSearchChannel / KeywordSearchChannel / GraphSearchChannel
 */
public final class ChunkRanking {

    private ChunkRanking() {
    }

    public static List<RetrievedChunk> mergeByScore(List<RetrievedChunk> primary, List<RetrievedChunk> supplement) {
        if (supplement.isEmpty()) {
            return sortedByScore(primary);
        }
        List<RetrievedChunk> merged = new ArrayList<>(primary.size() + supplement.size());
        merged.addAll(primary);
        merged.addAll(supplement);
        merged.sort(RetrievedChunk.BY_SCORE_DESC);
        return merged;
    }

    public static List<RetrievedChunk> sortedByScore(List<RetrievedChunk> chunks) {
        if (chunks.size() < 2) {
            return chunks;
        }
        List<RetrievedChunk> sorted = new ArrayList<>(chunks);
        sorted.sort(RetrievedChunk.BY_SCORE_DESC);
        return sorted;
    }

    public static float topScoreOf(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return 0F;
        }
        Float score = chunks.get(0).getScore();
        return score == null ? Float.NEGATIVE_INFINITY : score;
    }
}