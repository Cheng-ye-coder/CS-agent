package com.bitselect.agent.rag.core.prompt;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.rag.core.intent.NodeScore;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 【文件用途】Prompt 构建上下文：一次 RAG 请求中组装提示词所需的全部输入。
 *
 * 【为什么存在】
 * - 统一封装问题、MCP 上下文、KB 上下文、意图列表、可用意图 ID
 * - 提供 hasMcp() / hasKb() 便捷判断
 *
 * 【被谁引用】RAGPromptService / StreamChatPipeline / KnowledgeSearchFacade
 */
@Data
@Builder
public class PromptContext {

    /**
     * 用户原始问题。
     */
    private String question;

    /**
     * MCP 工具调用返回的上下文文本（已格式化）。
     */
    private String mcpContext;

    /**
     * 知识库检索返回的上下文文本（已格式化）。
     */
    private String kbContext;

    private List<NodeScore> mcpIntents;

    private List<NodeScore> kbIntents;

    @Builder.Default
    private Set<String> eligibleIntentIds = Set.of();

    public boolean hasMcp() {
        return StrUtil.isNotBlank(mcpContext);
    }

    public boolean hasKb() {
        return StrUtil.isNotBlank(kbContext);
    }
}