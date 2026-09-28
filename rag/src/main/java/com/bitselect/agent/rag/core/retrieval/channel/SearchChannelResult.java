package com.bitselect.agent.rag.core.retrieval.channel;

import com.bitselect.agent.framework.convention.RetrievedChunk;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】检索通道结果：单个通道的召回列表 + 元信息。
 *
 * 【为什么存在】
 * - 引擎需要区分"这条证据来自哪个通道"，融合与归因都依赖它
 * - 保留通道原始顺序（RRF 按名次取分，不按分数）
 *
 * 【被谁引用】MultiChannelRetrievalEngine / FusionPostProcessor / ChannelAttribution
 */
@Data
@Builder
public class SearchChannelResult {

    private SearchChannelType channelType;
    private String channelName;
    private List<RetrievedChunk> chunks;
    private long latencyMs;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}