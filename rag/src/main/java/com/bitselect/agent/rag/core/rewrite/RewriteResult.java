package com.bitselect.agent.rag.core.rewrite;

import java.util.List;

/**
 * 【文件用途】查询改写结果：改写后的单条查询 + 拆分出的子问题列表。
 *
 * 【为什么存在】
 * - 用户一次提问可能包含多个子问题（"请假流程是什么？报销怎么弄？"）
 * - 拆分成多个子问题后，每个子问题独立做意图识别和检索
 * - 改写后的查询更适合向量检索（去掉口语化的冗余表达）
 *
 * 【被谁引用】QueryRewriteService / IntentResolver / StreamChatPipeline / KnowledgeSearchFacade
 *
 * @param rewrittenQuestion 改写后的主查询
 * @param subQuestions      拆分出的子问题列表
 */
public record RewriteResult(String rewrittenQuestion, List<String> subQuestions) {
}