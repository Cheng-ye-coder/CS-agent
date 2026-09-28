package com.bitselect.agent.rag.dto;

/**
 * 【文件用途】SSE 流式响应的元信息事件载荷。
 *
 * 【为什么存在】
 * - SSE 开始后，前端需要立即知道"这是哪个会话、哪个流任务"
 * - 后续的取消操作需要 taskId，会话历史需要 conversationId
 * - 用一个元信息事件在流开始时下发，避免每个事件都带
 *
 * 【被谁引用】
 * - rag 的 SSE 流式输出（MetaEvent 的 data）
 */
public record MetaPayload(String conversationId, String taskId) {
}