package com.bitselect.agent.rag.dto;

import java.util.List;

/**
 * 【文件用途】推荐追问生成结果：AI 生成的一组"用户可能会接着问"的问题。
 *
 * 【为什么存在】
 * - 回答结束后展示推荐问题，提升用户探索深度
 * - 生成可能有三种结果：成功 / 空 / 失败，需要区分
 * - 用 Status 枚举表达状态，前端据此决定是否展示
 *
 * 【三种状态】
 * - SUCCESS：成功生成，questions 非空
 * - EMPTY：无推荐（如上下文不足）
 * - FAILED：生成失败
 *
 * 【关键设计】
 * - 工厂方法 success / empty / failed 让调用方语义清晰
 * - questions 做 null 归一，避免前端 NPE
 *
 * 【被谁引用】
 * - rag 的推荐问题生成节点
 * - SSE 流式输出的推荐问题事件
 */
public record RecommendedQuestionsPayload(Status status, List<String> questions) {

    public RecommendedQuestionsPayload {
        questions = questions == null ? List.of() : questions;
    }

    /**
     * 【工厂方法】成功：questions 非空时返回 SUCCESS，否则退化为 EMPTY。
     */
    public static RecommendedQuestionsPayload success(List<String> questions) {
        return questions == null || questions.isEmpty()
                ? empty()
                : new RecommendedQuestionsPayload(Status.SUCCESS, questions);
    }

    /**
     * 【工厂方法】无推荐。
     */
    public static RecommendedQuestionsPayload empty() {
        return new RecommendedQuestionsPayload(Status.EMPTY, List.of());
    }

    /**
     * 【工厂方法】生成失败。
     */
    public static RecommendedQuestionsPayload failed() {
        return new RecommendedQuestionsPayload(Status.FAILED, List.of());
    }

    /**
     * 【内部枚举】推荐结果状态。
     */
    public enum Status {
        SUCCESS,
        EMPTY,
        FAILED
    }
}