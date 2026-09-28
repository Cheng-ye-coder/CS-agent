package com.bitselect.agent.rag.dto;

import com.bitselect.agent.rag.core.intent.NodeScore;

import java.util.List;

/**
 * 【文件用途】意图分组：按 KB / MCP 两类汇总意图。
 *
 * 【为什么存在】
 * - KB 意图走检索链路，MCP 意图走工具调用链路
 * - 汇总后供 Prompt 编排区分处理
 *
 * 【被谁引用】IntentResolver / StreamChatPipeline / KnowledgeSearchFacade
 *
 * @param mcpIntents MCP 意图列表
 * @param kbIntents  KB 意图列表
 */
public record IntentGroup(List<NodeScore> mcpIntents, List<NodeScore> kbIntents) {
}