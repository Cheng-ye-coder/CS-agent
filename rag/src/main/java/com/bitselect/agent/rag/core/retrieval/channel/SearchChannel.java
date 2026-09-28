package com.bitselect.agent.rag.core.retrieval.channel;

import java.util.List;

/**
 * 【文件用途】检索通道接口：每个通道负责一种检索策略（向量 / 关键词 / 图谱 / 联网）。
 *
 * 【为什么存在】
 * - 多通道并行检索，结果统一进 RRF 融合
 * - 接口隔离实现，便于新增通道
 *
 * 【被谁引用】MultiChannelRetrievalEngine / VectorSearchChannel / KeywordSearchChannel / GraphSearchChannel / WebSearchChannel
 */
public interface SearchChannel {

    String getName();

    boolean isEnabled(SearchContext context);

    SearchChannelResult search(SearchContext context);

    SearchChannelType getType();

    /**
     * 【方法用途】空结果交卷：检索失败或无数据时的降级形态。
     */
    default SearchChannelResult emptyResult(long latencyMs) {
        return SearchChannelResult.builder()
                .channelType(getType())
                .channelName(getName())
                .chunks(List.of())
                .latencyMs(latencyMs)
                .build();
    }
}