package com.bitselect.agent.rag.core.prompt;

import com.bitselect.agent.framework.convention.RetrievedChunk;
import com.bitselect.agent.rag.core.intent.NodeScore;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】上下文格式化器接口：把检索结果和 MCP 工具结果格式化为可嵌入 Prompt 的文本。
 *
 * 【为什么存在】
 * - KB 检索结果和 MCP 结果都需要格式化后进 Prompt
 * - 接口隔离实现，便于替换格式化策略
 *
 * 【被谁引用】RetrievalEngine / DefaultContextFormatter
 */
public interface ContextFormatter {

    /**
     * 【方法用途】格式化知识库检索上下文。
     *
     * @param kbIntents         知识库意图节点及其得分列表
     * @param eligibleIntentIds 允许注入回答规则的意图 ID
     * @param rerankedChunks    后处理后的有序文档块
     * @param contextTopK       最终进 LLM 的文档块条数上限
     */
    String formatKbContext(List<NodeScore> kbIntents,
                           Set<String> eligibleIntentIds,
                           List<RetrievedChunk> rerankedChunks,
                           int contextTopK);

    /**
     * 【方法用途】格式化 MCP 工具调用上下文。
     */
    String formatMcpContext(Map<String, List<CallToolResult>> toolResults, List<NodeScore> mcpIntents);
}