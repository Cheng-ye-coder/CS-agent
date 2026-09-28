package com.bitselect.agent.rag.core.prompt;

/**
 * 【文件用途】Prompt 构建场景枚举：根据检索来源确定系统提示词模板。
 *
 * 【为什么存在】
 * - 四种场景对应不同的系统提示词：KB_ONLY / MCP_ONLY / MIXED / EMPTY
 * - RAGPromptService 的 plan() 方法据此选择模板
 *
 * 【被谁引用】RAGPromptService / PromptBuildPlan
 */
public enum PromptScene {

    /**
     * 仅命中知识库检索，使用企业知识库专用提示词模板。
     */
    KB_ONLY,

    /**
     * 仅命中 MCP 工具调用，使用 MCP 专用提示词模板。
     */
    MCP_ONLY,

    /**
     * 同时命中知识库和 MCP，使用混合提示词模板。
     */
    MIXED,

    /**
     * 无任何检索命中，返回空提示词。
     */
    EMPTY
}