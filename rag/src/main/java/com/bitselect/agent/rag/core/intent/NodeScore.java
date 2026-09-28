package com.bitselect.agent.rag.core.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】意图节点打分：LLM 给某个叶子意图节点的匹配分数。
 *
 * 【为什么存在】
 * - 意图分类的产出是一组"节点 + 分数"
 * - 下游按分数排序、过滤、分组
 * - 是整个 intent / retrieval / dto 包的公共数据契约
 *
 * 【被谁引用】IntentClassifier / IntentResolver / NodeScoreFilters / RetrievalScopeResolver
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NodeScore {

    private IntentNode node;

    private double score;
}