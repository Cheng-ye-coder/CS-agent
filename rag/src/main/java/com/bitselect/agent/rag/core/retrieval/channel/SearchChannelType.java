package com.bitselect.agent.rag.core.retrieval.channel;

/**
 * 【文件用途】检索通道类型枚举。
 *
 * 【为什么存在】
 * - 标记检索来源（向量 / 关键词 / 图谱 / 联网 / 混合）
 * - 融合阶段按类型赋不同权重（fusion.channel-weights）
 * - 归因日志按类型统计贡献与存活率
 *
 * 【被谁引用】SearchChannel / SearchChannelResult / FusionPostProcessor / ChannelAttribution
 */
public enum SearchChannelType {

    VECTOR,
    KEYWORD,
    GRAPH,
    WEB_SEARCH,
    HYBRID
}