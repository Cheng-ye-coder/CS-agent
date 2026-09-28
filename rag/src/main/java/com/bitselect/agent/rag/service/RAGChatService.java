package com.bitselect.agent.rag.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 【文件用途】RAG 对话服务接口：对外暴露流式问答与任务停止能力。
 *
 * 【为什么存在】
 * - Controller 层只依赖接口，不关心实现细节
 * - 流式问答需要 SSE，停止任务需要 taskId
 * - 接口让后续可以替换实现（如 Agent 模式 vs 传统 RAG 模式）
 *
 * 【被谁引用】
 * - rag/controller（REST 接口）
 */
public interface RAGChatService {

    /**
     * 【方法用途】发起一次 SSE 流式问答。
     *
     * @param question       用户问题
     * @param conversationId 会话 ID（可选，空时创建新会话）
     * @param deepThinking   是否开启深度思考模式
     * @param emitter        SSE 发射器
     */
    void streamChat(String question, String conversationId, Boolean deepThinking, SseEmitter emitter);

    /**
     * 【方法用途】停止指定任务 ID 的流式会话。
     *
     * @param taskId 任务 ID
     */
    void stopTask(String taskId);
}