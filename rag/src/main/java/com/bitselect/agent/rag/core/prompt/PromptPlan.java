package com.bitselect.agent.rag.core.prompt;

import com.bitselect.agent.rag.core.intent.NodeScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 【文件用途】Prompt 模板选择计划：单意图时选自定义模板，多意图时用默认模板。
 *
 * 【为什么存在】
 * - 单意图且节点配置了 promptTemplate 时用节点模板
 * - 否则用场景默认模板
 *
 * 【被谁引用】RAGPromptService.planPrompt()
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class PromptPlan {

    private List<NodeScore> retainedIntents;
    private String baseTemplate;
}