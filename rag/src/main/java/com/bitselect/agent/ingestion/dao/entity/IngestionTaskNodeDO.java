package com.bitselect.agent.ingestion.dao.entity;

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
 * 【文件用途】知识库数据接入任务节点实体：一次 Pipeline 执行中，单个节点的执行记录。
 *
 * 【为什么存在】
 * - 一条任务（IngestionTaskDO）包含多个节点的执行（IngestionTaskNodeDO）
 * - 每个节点需要记录耗时、状态、输出，用于排障
 * - 一次任务失败时，能精确知道是哪个节点出的问题
 *
 * 【数据库表】t_ingestion_task_node
 *
 * 【关键字段】
 * - taskId：属于哪次任务
 * - nodeId / nodeType：哪个节点
 * - nodeOrder：节点顺序
 * - status：success / failed / skipped
 * - durationMs：节点耗时
 * - outputJson：节点输出（JSON）
 *
 * 【被谁引用】
 * - IngestionTaskNodeMapper（CRUD）
 * - IngestionService（节点级日志记录）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_ingestion_task_node")
public class IngestionTaskNodeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属任务 ID
     */
    private String taskId;

    /**
     * 所属 Pipeline ID
     */
    private String pipelineId;

    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 节点类型：fetcher / parser / chunker / indexer 等
     */
    private String nodeType;

    /**
     * 节点在 Pipeline 中的执行顺序
     */
    private Integer nodeOrder;

    /**
     * 节点状态：success / failed / skipped
     */
    private String status;

    /**
     * 节点耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 执行消息（如"解析完成，产出 42 个 Block"）
     */
    private String message;

    /**
     * 错误消息（status=failed 时）
     */
    private String errorMessage;

    /**
     * 节点输出 JSON（如解析结果、分块结果）
     */
    private String outputJson;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}