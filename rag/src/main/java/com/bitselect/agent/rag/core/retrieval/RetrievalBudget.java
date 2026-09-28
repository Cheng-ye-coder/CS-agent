package com.bitselect.agent.rag.core.retrieval;

/**
 * 【文件用途】检索漏斗的三段预算：召回扇出 / Rerank 候选池上限 / 最终条数。
 *
 * 【为什么存在】
 * - 一条 retrieve → fuse → rerank → render 链路本有三个方向与成本各异、须各自独立的预算
 * - 以往被一个 topK 复用，调一处误动三处
 * - 显式拆成三段、一次算好、各阶段只读属于自己的那一段
 *
 * 【三段预算】
 * - recallBudget  — 每通道 fan-out 基数（想大、保召回）
 * - candidateLimit — 融合后送 Rerank 的候选池上限（成本天花板）
 * - contextTopK   — 最终进 LLM 的条数（想小而精，即产品语义的 topK）
 *
 * 【不变式】recallBudget ≥ contextTopK 且 candidateLimit ≥ contextTopK
 *
 * 【被谁引用】MultiChannelRetrievalEngine / RetrievalEngine
 */
public record RetrievalBudget(int recallBudget, int candidateLimit, int contextTopK) {

    /**
     * 【工厂方法】三段同值构造：用于测试或无需区分预算的平凡场景。
     */
    public static RetrievalBudget uniform(int k) {
        return new RetrievalBudget(k, k, k);
    }
}