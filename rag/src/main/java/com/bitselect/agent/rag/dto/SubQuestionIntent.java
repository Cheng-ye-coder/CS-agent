package com.bitselect.agent.rag.dto;

import com.bitselect.agent.rag.core.intent.NodeScore;

import java.util.List;

/**
 * 【文件用途】子问题与其意图候选：一个子问题对应的所有意图打分。
 *
 * 【为什么存在】
 * - 多子问题场景下，每个子问题独立做检索和意图解析
 * - 保留下标和文本，供下游按子问题分组处理
 *
 * 【被谁引用】IntentResolver / RetrievalEngine / StreamChatPipeline / KnowledgeSearchFacade
 *
 * @param subQuestion 子问题文本
 * @param nodeScores  子问题的意图候选
 */
public record SubQuestionIntent(String subQuestion, List<NodeScore> nodeScores) {
}