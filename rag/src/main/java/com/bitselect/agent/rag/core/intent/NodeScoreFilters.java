package com.bitselect.agent.rag.core.intent;

import cn.hutool.core.util.StrUtil;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 【文件用途】NodeScore 过滤工具：统一 KB / MCP 意图的过滤逻辑。
 *
 * 【为什么存在】
 * - 多个调用点需要"过滤出 KB 意图""过滤出 MCP 意图""提取 KB 的 collection 列表"
 * - 集中一处避免逻辑重复
 *
 * 【被谁引用】IntentResolver / RetrievalScopeResolver / RetrievalEngine / KnowledgeSearchFacade
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class NodeScoreFilters {

    /**
     * 【方法用途】过滤 MCP 类型意图（node 非空、kind=MCP、mcpToolId 非空）。
     */
    public static List<NodeScore> mcp(List<NodeScore> scores) {
        return scores.stream()
                .filter(ns -> ns.getNode() != null && ns.getNode().isMCP())
                .filter(ns -> StrUtil.isNotBlank(ns.getNode().getMcpToolId()))
                .toList();
    }

    /**
     * 【方法用途】过滤 KB 类型意图（node 非空、kind 为 null 或 KB）。
     */
    public static List<NodeScore> kb(List<NodeScore> scores) {
        return scores.stream()
                .filter(ns -> ns.getNode() != null && ns.getNode().isKB())
                .toList();
    }

    /**
     * 【方法用途】过滤 KB 类型意图并限制最低分数。
     */
    public static List<NodeScore> kb(List<NodeScore> scores, double minScore) {
        return scores.stream()
                .filter(ns -> ns.getScore() >= minScore)
                .filter(ns -> ns.getNode() != null && ns.getNode().isKB())
                .toList();
    }

    /**
     * 【方法用途】提取 KB 意图对应的 collection 名称（去空、去重）。
     */
    public static List<String> kbCollections(List<NodeScore> scores) {
        return kb(scores).stream()
                .flatMap(ns -> ns.getNode().getEffectiveCollectionNames().stream())
                .distinct()
                .toList();
    }
}