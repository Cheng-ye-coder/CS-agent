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
 * 【文件用途】智能体提示词实体：每个智能体的每个"槽位"（人设 / 回答规则 / 推荐问题）一条记录。
 *
 * 【为什么存在】
 * - 提示词按"槽位"拆分，不同槽位在不同架构阶段生效
 * - 空白内容视为未配置，回落内置智能体
 *
 * 【数据库表】t_agent_prompt
 *
 * 【被谁引用】AgentPromptMapper、AgentProfileAdminService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_agent_prompt")
public class AgentPromptDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String agentId;

    /**
     * 槽位标识，取值见 AgentPromptSlot
     */
    private String slotKey;

    /**
     * 提示词全文，空白视为未配置并回落内置智能体
     */
    private String content;

    private String createBy;
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}