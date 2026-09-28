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
 * 【文件用途】智能体配置实体：定义"一套人设 + 一组提示词"。
 *
 * 【为什么存在】
 * - 同一个系统可以有多个智能体（客服 / HR 助手 / IT 运维），每个有人设和提示词
 * - 内置智能体（builtin=1）是所有空槽位的回落终点
 * - 全局仅允许一条激活（active=1）
 *
 * 【数据库表】t_agent_profile
 *
 * 【被谁引用】AgentProfileMapper、AgentProfileAdminService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_agent_profile")
public class AgentProfileDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String description;

    /**
     * 头像预设标识，取值由前端预设表定义
     */
    private String avatar;

    /**
     * 是否内置：内置智能体不可编辑不可删除，是所有空槽位的回落终点
     */
    private Integer builtin;

    /**
     * 是否激活，全局仅允许一条为 1
     */
    private Integer active;

    private String createBy;
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}