package com.bitselect.agent.rag.core.retrieval.channel;

import com.bitselect.agent.rag.core.intent.NodeScore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 【文件用途】检索作用域：每个子问题算一次，向量 / 关键词 / 图谱共读一份。
 *
 * 【为什么存在】
 * - KB 意图足够置信则收窄到命中库（定向），否则退化为全库（全局）
 * - 定向下 supplementCollections 是未命中库，向量通道用它并行补一路
 *
 * 【被谁引用】MultiChannelRetrievalEngine / VectorSearchChannel / KeywordSearchChannel / GraphSearchChannel
 */
public record RetrievalScope(boolean directed,
                             double topScore,
                             List<NodeScore> intents,
                             List<String> targetCollections,
                             List<String> supplementCollections) {

    /**
     * 【方法用途】定向时返回命中意图 ID 集合，全局时返回空集。
     */
    public Set<String> directedIntentIds() {
        if (!directed) {
            return Set.of();
        }
        Set<String> intentIds = new LinkedHashSet<>();
        for (NodeScore intent : intents) {
            if (intent == null || intent.getNode() == null) {
                continue;
            }
            String intentId = intent.getNode().getId();
            if (intentId == null || intentId.isBlank()) {
                continue;
            }
            intentIds.add(intentId);
        }
        return Set.copyOf(intentIds);
    }

    /**
     * 【工厂方法】全局作用域：不收窄，无补充路。
     */
    public static RetrievalScope global(double topScore, List<String> activeCollections) {
        return new RetrievalScope(false, topScore, List.of(), activeCollections, List.of());
    }
}