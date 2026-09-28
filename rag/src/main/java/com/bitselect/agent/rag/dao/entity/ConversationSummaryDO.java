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
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 【文件用途】会话摘要实体：一次上下文压缩产出的摘要。
 *
 * 【为什么存在】
 * - 长会话需要压缩历史（滑动窗口 + 摘要），摘要要落库
 * - lastMessageId 记录"摘要覆盖到哪条消息"，避免重复摘要
 *
 * 【数据库表】t_conversation_summary
 *
 * 【关键设计】
 * - @Accessors(chain = true)：链式 setter，Service 层聚合时方便
 *
 * 【被谁引用】ConversationSummaryMapper、ConversationMessageService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("t_conversation_summary")
public class ConversationSummaryDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String conversationId;

    private String userId;

    private String content;

    /**
     * 摘要覆盖的最后一条消息 ID
     */
    private String lastMessageId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}