package com.bitselect.agent.rag.core.intent;

import java.util.List;

/**
 * 【文件用途】意图分类器接口：对用户问题做意图识别。
 *
 * 【为什么存在】
 * - 支持多种实现策略（串行 / 并行），接口隔离实现
 * - 默认实现 DefaultIntentClassifier 用 LLM 单次调用完成分类
 *
 * 【被谁引用】IntentResolver
 */
public interface IntentClassifier {

    /**
     * 【方法用途】对所有叶子分类节点做意图识别，返回按 score 降序的打分列表。
     */
    List<NodeScore> classifyTargets(String question);

    /**
     * 【方法用途】取前 topN 个且 score >= minScore 的分类。
     */
    default List<NodeScore> topKAboveThreshold(String question, int topN, double minScore) {
        return classifyTargets(question).stream()
                .filter(ns -> ns.getScore() >= minScore)
                .limit(topN)
                .toList();
    }
}