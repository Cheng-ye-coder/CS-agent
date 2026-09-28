package com.bitselect.agent.rag.service.bo;

import com.bitselect.agent.framework.convention.GroundingChunk;
import com.bitselect.agent.framework.convention.SourceRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 【文件用途】对话消息业务对象：承载一条消息的完整内容与元数据。
 *
 * 【为什么存在】
 * - 消息需要携带的内容远不止"角色 + 文本"：思考内容、来源、grounding 片段、回复关系、结束状态
 * - 用 BO 表达"一次消息写入的完整意图"
 *
 * 【被谁引用】ConversationMessageService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessageBO {

    private String conversationId;

    private String userId;

    /**
     * 角色：system / user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 深度思考内容
     */
    private String thinkingContent;

    /**
     * 深度思考耗时（秒）
     */
    private Integer thinkingDuration;

    /**
     * 回答来源（文档级来源列表，仅 assistant 消息可能有）
     */
    private List<SourceRef> sources;

    /**
     * 推荐问题 grounding 片段（仅 assistant 消息可能有）
     */
    private List<GroundingChunk> retrievedChunks;

    /**
     * 当前助手消息对应的用户消息 ID
     */
    private String replyToMessageId;

    /**
     * 消息结束状态：NORMAL / INTERRUPTED / REJECTED
     */
    private String messageStatus;
}