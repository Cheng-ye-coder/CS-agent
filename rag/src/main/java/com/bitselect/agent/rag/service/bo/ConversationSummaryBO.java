package com.bitselect.agent.rag.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】会话摘要业务对象：一次上下文压缩产出的摘要。
 *
 * 【为什么存在】
 * - 长会话需要压缩历史（滑动窗口 + 摘要），摘要本身要落库
 * - lastMessageId 记录"摘要覆盖到哪条消息"，避免重复摘要
 *
 * 【被谁引用】ConversationMessageService.addMessageSummary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryBO {

    private String conversationId;

    private String userId;

    /**
     * 摘要内容
     */
    private String content;

    /**
     * 摘要覆盖的最后一条消息 ID
     */
    private String lastMessageId;
}