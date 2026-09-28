package com.bitselect.agent.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 【文件用途】消息反馈实体：用户对助手消息的点赞 / 点踩。
 *
 * 【为什么存在】
 * - 收集用户反馈用于质量分析
 * - 一条消息一个用户只能有一条有效反馈（靠唯一约束 + upsert 保证）
 *
 * 【数据库表】t_message_feedback
 *
 * 【被谁引用】MessageFeedbackMapper、MessageFeedbackService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_message_feedback")
public class MessageFeedbackDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String messageId;

    private String conversationId;

    private String userId;

    /**
     * 反馈值：1=点赞，-1=点踩
     */
    private Integer vote;

    private String reason;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}