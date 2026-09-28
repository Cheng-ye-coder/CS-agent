package com.bitselect.agent.ingestion.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bitselect.agent.framework.database.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 【文件用途】摄取流水线节点实体：Pipeline 中的单个处理步骤。
 *
 * 【为什么存在】
 * - 一条 Pipeline 由多个节点串联组成，每个节点负责一个处理步骤
 * - 节点类型决定"做什么"（fetcher / parser / chunker / indexer 等）
 * - settingsJson 保存节点配置（如分块预算、解析档位）
 * - conditionJson 保存执行条件（如"仅当 MIME 是 PDF 时执行"）
 *
 * 【数据库表】t_ingestion_pipeline_node
 *
 * 【关键字段】
 * - pipelineId：属于哪条 Pipeline
 * - nodeId：节点在 Pipeline 内的 ID
 * - nodeType：节点类型
 * - nextNodeId：下一个节点，构成链表
 * - settingsJson / conditionJson：JSONB 存储，用 JsonbTypeHandler 处理
 *
 * 【被谁引用】
 * - IngestionPipelineNodeMapper（CRUD）
 * - PipelineEngine（执行时遍历节点）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_ingestion_pipeline_node")
public class IngestionPipelineNodeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属 Pipeline ID
     */
    private String pipelineId;

    /**
     * 节点在 Pipeline 内的 ID（用户定义）
     */
    private String nodeId;

    /**
     * 节点类型：fetcher / parser / chunker / indexer 等
     */
    private String nodeType;

    /**
     * 下一个节点 ID，构成链表
     */
    private String nextNodeId;

    /**
     * 节点配置 JSON（如分块预算、解析档位）
     *
     * JSONB 存储，用 JsonbTypeHandler 处理
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String settingsJson;

    /**
     * 节点执行条件 JSON（如"仅当 MIME 是 PDF 时执行"）
     *
     * JSONB 存储，用 JsonbTypeHandler 处理
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String conditionJson;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}