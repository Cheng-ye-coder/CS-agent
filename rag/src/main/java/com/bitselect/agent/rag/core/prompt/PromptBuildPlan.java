package com.bitselect.agent.rag.core.prompt;

import lombok.Builder;
import lombok.Data;

/**
 * 【文件用途】Prompt 构建计划：RAGPromptService 内部使用的中转对象。
 *
 * 【为什么存在】
 * - plan() 方法的返回值，携带场景、模板、上下文
 * - 让 buildSystemPrompt() 的入参更清晰
 *
 * 【被谁引用】RAGPromptService
 */
@Data
@Builder
public class PromptBuildPlan {

    private PromptScene scene;
    private String baseTemplate;
    private String mcpContext;
    private String kbContext;
    private String question;
}