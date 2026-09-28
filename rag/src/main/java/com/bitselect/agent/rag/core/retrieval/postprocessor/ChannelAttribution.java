package com.bitselect.agent.rag.core.retrieval.postprocessor;

import com.bitselect.agent.framework.convention.RetrievedChunk;
import com.bitselect.agent.framework.convention.RetrievedChunkKey;
import com.bitselect.agent.rag.core.retrieval.channel.SearchChannelResult;
import com.bitselect.agent.rag.core.retrieval.channel.SearchChannelType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】检索通道归因工具：反查每条证据来自哪些通道。
 *
 * 【为什么存在】
 * - RetrievedChunk 不携带来源通道字段（框架层 DTO 保持纯净）
 * - 融合 / Rerank 需要打印"各通道贡献 / 存活率"的可观测日志
 * - 按 chunk key 从不可变的 SearchChannelResult 反查
 *
 * 【被谁引用】FusionPostProcessor / RerankPostProcessor
 */
final class ChannelAttribution {

    private ChannelAttribution() {
    }

    static Map<String, Set<SearchChannelType>> index(List<SearchChannelResult> results) {
        Map<String, Set<SearchChannelType>> index = new HashMap<>();
        if (results == null) {
            return index;
        }
        for (SearchChannelResult result : results) {
            if (result == null || result.getChunks() == null) {
                continue;
            }
            for (RetrievedChunk chunk : result.getChunks()) {
                index.computeIfAbsent(RetrievedChunkKey.of(chunk), k -> EnumSet.noneOf(SearchChannelType.class))
                        .add(result.getChannelType());
            }
        }
        return index;
    }

    static Map<SearchChannelType, Integer> countByChannel(List<RetrievedChunk> chunks,
                                                          Map<String, Set<SearchChannelType>> index) {
        Map<SearchChannelType, Integer> counts = new EnumMap<>(SearchChannelType.class);
        for (RetrievedChunk chunk : chunks) {
            Set<SearchChannelType> channels = index.get(RetrievedChunkKey.of(chunk));
            if (channels == null) {
                continue;
            }
            for (SearchChannelType channel : channels) {
                counts.merge(channel, 1, Integer::sum);
            }
        }
        return counts;
    }

    static long countOfChannel(List<RetrievedChunk> chunks,
                               Map<String, Set<SearchChannelType>> index,
                               SearchChannelType channel) {
        return chunks.stream()
                .map(RetrievedChunkKey::of)
                .map(index::get)
                .filter(set -> set != null && set.contains(channel))
                .count();
    }

    static String format(Map<SearchChannelType, Integer> counts) {
        if (counts.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        counts.forEach((type, n) -> sb.append(label(type)).append('=').append(n).append(' '));
        return sb.toString().trim();
    }

    static String label(SearchChannelType type) {
        return switch (type) {
            case VECTOR -> "向量";
            case KEYWORD -> "关键词";
            case GRAPH -> "图谱";
            case WEB_SEARCH -> "联网";
            case HYBRID -> "混合";
        };
    }
}