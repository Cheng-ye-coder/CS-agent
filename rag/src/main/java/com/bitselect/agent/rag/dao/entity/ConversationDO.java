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
 * 【文件用途】会话实体：一次用户与助手的多轮对话记录。
 *
 * 【为什么存在】
 * - 用户每次开启新对话就是一个会话
 * - 会话需要标题（首次提问后 LLM 生成）
 * - 会话列表按 lastTime 倒序展示
 *
 * 【数据库表】t_conversation
 *
 * 【被谁引用】ConversationMapper、ConversationService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_conversation")
public class ConversationDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String conversationId;

    private String userId;

    private String title;

    private Date lastTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}