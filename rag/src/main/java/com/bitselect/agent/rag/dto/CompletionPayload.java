package com.bitselect.agent.rag.dto;

import com.bitselect.agent.framework.convention.ChatMessage.MessageStatus;
import com.bitselect.agent.framework.convention.SourceRef;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 【文件用途】模型回复完成事件的载荷：SSE 流式输出的终结事件携带的数据。
 *
 * 【为什么存在】
 * - 流式回答结束时，前端需要知道"消息 ID、标题、来源列表、结束状态"
 * - 用统一结构承载这些信息，前端解析一次即可
 * - 用 @JsonInclude(NON_NULL) 让空字段不序列化，减小 SSE 载荷
 *
 * 【关键设计】
 * - messageId 用字符串而非数字：前端 JS 的 Number 精度不足，雪花 ID 会丢精度
 * - 提供便捷构造 CompletionPayload(messageId, title) 用于无来源场景
 *
 * 【被谁引用】
 * - rag 的 SSE 流式输出（CompletionEvent 的 data）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompletionPayload(String messageId, String title, List<SourceRef> sources, MessageStatus messageStatus) {

    /**
     * 【便捷构造】无来源场景：sources 置空（NON_NULL 序列化时自动省略该字段）。
     */
    public CompletionPayload(String messageId, String title) {
        this(messageId, title, null, MessageStatus.NORMAL);
    }
}