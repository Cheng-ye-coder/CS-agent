package com.bitselect.agent.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bitselect.agent.framework.convention.GroundingChunk;
import com.bitselect.agent.framework.convention.SourceRef;
import com.bitselect.agent.knowledge.dao.handler.GroundingChunkListTypeHandler;
import com.bitselect.agent.knowledge.dao.handler.SourceRefListTypeHandler;
import com.bitselect.agent.knowledge.dao.handler.StringListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 【文件用途】会话消息实体：存储对话过程中的每一条消息。
 *
 * 【为什么存在】
 * - 每条消息需要携带：角色、内容、思考内容、来源、grounding 片段、推荐问题
 * - 多轮对话按 ID 递增查询历史
 *
 * 【数据库表】t_message
 *
 * 【关键设计】
 * - autoResultMap=true：启用自定义 TypeHandler
 * - sources / retrievedChunks / recommendedQuestions 用 jsonb 存储
 * - messageStatus 标记消息结束状态，供推荐问题生成判断
 *
 * 【被谁引用】ConversationMessageMapper、ConversationMessageService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "t_message", autoResultMap = true)
public class ConversationMessageDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String conversationId;

    private String userId;

    /**
     * 角色：user / assistant
     */
    private String role;

    private String content;

    private String thinkingContent;

    private Integer thinkingDuration;

    /**
     * 回答来源，文档级来源列表（jsonb 存储，仅 assistant 消息可能有）
     */
    @TableField(typeHandler = SourceRefListTypeHandler.class)
    private List<SourceRef> sources;

    /**
     * 推荐问题 grounding 片段（jsonb 存储，仅 assistant 消息可能有）
     */
    @TableField(typeHandler = GroundingChunkListTypeHandler.class)
    private List<GroundingChunk> retrievedChunks;

    /**
     * 推荐追问问题，答案后懒加载生成（jsonb 存储，仅 assistant 消息可能有）
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> recommendedQuestions;

    /**
     * 当前助手消息对应的用户消息 ID
     */
    private String replyToMessageId;

    /**
     * 消息结束状态：NORMAL / INTERRUPTED / REJECTED
     */
    private String messageStatus;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}