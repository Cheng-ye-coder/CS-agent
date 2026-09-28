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
 * 【文件用途】意图树节点实体：客服系统的意图分类树。
 *
 * 【为什么存在】
 * - 用户问题需要先分类到某个意图（如"退款咨询" / "订单查询"）
 * - 意图树有层级：DOMAIN → CATEGORY → TOPIC
 * - 每个节点对应一类知识库、一个系统提示、或一个 MCP 工具
 *
 * 【数据库表】t_intent_node
 *
 * 【关键字段】
 * - intentCode / name：业务标识与展示名
 * - level / parentCode：层级结构
 * - kind：0=KB（RAG）/ 1=SYSTEM / 2=MCP
 * - collectionNames：关联的知识库集合列表
 * - mcpToolId / requireConfirm：MCP 工具配置
 * - promptTemplate / paramPromptTemplate：提示词模板
 *
 * 【被谁引用】IntentNodeMapper、IntentNodeRegistry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value = "t_intent_node", autoResultMap = true)
public class IntentNodeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String kbId;

    /**
     * 业务唯一标识，如 group-hr / biz-oa-intro
     */
    private String intentCode;

    private String name;

    /**
     * 层级：0=DOMAIN，1=CATEGORY，2=TOPIC
     */
    private Integer level;

    /**
     * 父节点的 intent_code
     */
    private String parentCode;

    private String description;

    /**
     * 示例问题：JSON 数组字符串
     */
    private String examples;

    /**
     * Milvus Collection 名称（仅对 kind=0 有意义）
     */
    private String collectionName;

    /**
     * 关联的 Collection 名称列表（JSONB）
     * collectionName 仅保留用于兼容旧数据，新逻辑以此字段为准
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> collectionNames;

    /**
     * MCP 工具 ID（仅对 kind=2 有意义）
     */
    private String mcpToolId;

    /**
     * 执行前是否需要用户确认：0=否，1=是（仅对 kind=2 有意义）
     */
    private Integer requireConfirm;

    private Integer topK;

    /**
     * 类型：0=KB（RAG），1=SYSTEM，2=MCP
     */
    private Integer kind;

    private Integer sortOrder;

    /**
     * 短规则片段（可选）
     */
    private String promptSnippet;

    /**
     * 场景用的完整 Prompt 模板（可选）
     */
    private String promptTemplate;

    /**
     * 参数提取提示词模板（MCP 模式专属）
     */
    private String paramPromptTemplate;

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