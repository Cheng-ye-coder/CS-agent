package com.bitselect.agent.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bitselect.agent.knowledge.dao.handler.StringListTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 【文件用途】智能体技能实体：写给模型看的操作手册，正文纯 Markdown。
 *
 * 【为什么存在】
 * - 技能是"按需加载"的操作手册：模型只在需要时加载某个技能正文
 * - 技能激活时会解锁一批 MCP 工具（toolIds）
 *
 * 【数据库表】t_agent_skill
 *
 * 【关键设计】
 * - autoResultMap=true：启用自定义 TypeHandler
 * - toolIds 用 StringListTypeHandler 存储为 jsonb 数组
 *
 * 【被谁引用】AgentSkillMapper、AgentSkillAdminService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "t_agent_skill", autoResultMap = true)
public class AgentSkillDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 技能标识，模型加载正文时报的名字，如 leave_apply
     */
    private String skillCode;

    private String name;

    /**
     * 适用场景，随技能清单一起交给模型判断要不要加载正文
     */
    private String description;

    /**
     * 技能正文 Markdown
     */
    private String content;

    /**
     * 加载本技能后才解锁的 MCP 工具 ID，取值只能来自意图树的 MCP 节点
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> toolIds;

    private Integer sortOrder;

    private Integer enabled;

    private String createBy;
    private String updateBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}