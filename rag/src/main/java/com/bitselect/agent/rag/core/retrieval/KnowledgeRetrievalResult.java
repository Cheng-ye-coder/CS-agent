package com.bitselect.agent.rag.core.retrieval;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.framework.convention.RetrievedChunk;
import com.bitselect.agent.framework.convention.RetrievedChunkKey;
import com.bitselect.agent.rag.core.intent.IntentNode;
import com.bitselect.agent.rag.core.intent.NodeScore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【文件用途】知识库检索结果：一次 KB 检索的完整产出。
 *
 * 【为什么存在】
 * - 携带三种信息：后处理后的 chunk 列表、意图归属映射、定向意图白名单
 * - 是 MultiChannelRetrievalEngine → RetrievalEngine 之间的输出契约
 *
 * 【被谁引用】MultiChannelRetrievalEngine / RetrievalEngine
 */
public record KnowledgeRetrievalResult(List<RetrievedChunk> chunks,
                                       Map<String, Set<String>> intentIdsByChunkKey,
                                       Set<String> directedIntentIds) {

    public KnowledgeRetrievalResult {
        chunks = chunks == null ? List.of() : chunks;
        intentIdsByChunkKey = intentIdsByChunkKey == null ? Map.of() : intentIdsByChunkKey;
        directedIntentIds = directedIntentIds == null
                ? Set.of()
                : Set.copyOf(directedIntentIds);
    }

    /**
     * 【工厂方法】空结果。
     */
    public static KnowledgeRetrievalResult empty() {
        return new KnowledgeRetrievalResult(List.of(), Map.of(), Set.of());
    }

    /**
     * 【方法用途】本次检索命中的意图 ID 集合。
     */
    public Set<String> retrievedIntentIds() {
        Set<String> intentIds = new LinkedHashSet<>();
        intentIdsByChunkKey.values().stream()
                .filter(Objects::nonNull)
                .forEach(intentIds::addAll);
        return Collections.unmodifiableSet(intentIds);
    }

    /**
     * 【方法用途】筛选出可用的意图 ID 白名单。
     */
    public Set<String> eligibleIntentIds(List<NodeScore> candidateIntents) {
        Set<String> retrievedIntentIds = retrievedIntentIds();
        return candidateIntents.stream()
                .filter(Objects::nonNull)
                .map(NodeScore::getNode)
                .filter(Objects::nonNull)
                .map(IntentNode::getId)
                .filter(StrUtil::isNotBlank)
                .filter(intentId -> !directedIntentIds.contains(intentId)
                        || retrievedIntentIds.contains(intentId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 【方法用途】按意图分组 chunk：未归属任何意图的 chunk 归入 globalKey。
     */
    public Map<String, List<RetrievedChunk>> groupByIntent(String globalKey) {
        Map<String, List<RetrievedChunk>> grouped = new LinkedHashMap<>();
        for (RetrievedChunk chunk : chunks) {
            Set<String> intentIds = intentIdsByChunkKey.get(RetrievedChunkKey.of(chunk));
            if (intentIds == null || intentIds.isEmpty()) {
                grouped.computeIfAbsent(globalKey, ignored -> new ArrayList<>()).add(chunk);
                continue;
            }
            for (String intentId : intentIds) {
                grouped.computeIfAbsent(intentId, ignored -> new ArrayList<>()).add(chunk);
            }
        }
        return grouped;
    }
}