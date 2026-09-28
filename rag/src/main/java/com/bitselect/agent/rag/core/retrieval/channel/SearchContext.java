package com.bitselect.agent.rag.core.retrieval.channel;

import com.bitselect.agent.rag.core.retrieval.RetrievalBudget;
import com.bitselect.agent.rag.dto.SubQuestionIntent;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】检索上下文：携带检索所需的所有信息，在多个通道之间传递。
 *
 * 【为什么存在】
 * - 通道需要知道：问题、意图、预算、作用域
 * - 统一封装避免参数散落
 *
 * 【被谁引用】MultiChannelRetrievalEngine / 所有 SearchChannel 实现
 */
@Data
@Builder
public class SearchContext {

    private String originalQuestion;

    private String rewrittenQuestion;

    private List<String> subQuestions;

    private List<SubQuestionIntent> intents;

    private RetrievalBudget budget;

    private RetrievalScope retrievalScope;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 【方法用途】获取主问题（优先使用重写后的问题）。
     */
    public String getMainQuestion() {
        return rewrittenQuestion != null ? rewrittenQuestion : originalQuestion;
    }
}