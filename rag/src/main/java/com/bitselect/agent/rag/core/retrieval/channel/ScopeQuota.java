package com.bitselect.agent.rag.core.retrieval.channel;

import java.util.List;

/**
 * 【文件用途】主路与补充路的候选名额划分。
 *
 * 【为什么存在】
 * - 三条通道共用一条规则：各自把产出额度按同一比例切一片给未命中库
 * - 补充证据必须有固定名额而非与命中库证据自由竞争
 *
 * 【被谁引用】VectorSearchChannel / KeywordSearchChannel / GraphSearchChannel
 */
public record ScopeQuota(int primary, int supplement) {

    /**
     * 【方法用途】按作用域切分通道产出额度。
     */
    public static ScopeQuota split(RetrievalScope scope, int budget, double supplementRatio) {
        if (!scope.directed() || scope.supplementCollections().isEmpty() || supplementRatio <= 0 || budget <= 0) {
            return new ScopeQuota(budget, 0);
        }
        int supplement = Math.min(budget - 1, Math.max(1, (int) Math.round(budget * supplementRatio)));
        return new ScopeQuota(budget - supplement, supplement);
    }

    /**
     * 【方法用途】按名额截断已按相关性降序的候选，名额为 0 即取零条。
     */
    public static <T> List<T> cap(List<T> chunks, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return chunks.size() > limit ? List.copyOf(chunks.subList(0, limit)) : chunks;
    }
}