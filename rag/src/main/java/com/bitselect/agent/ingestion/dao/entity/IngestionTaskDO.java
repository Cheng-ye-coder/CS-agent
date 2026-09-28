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
 * 【文件用途】知识库摄取任务实体：一次 Pipeline 执行的记录。
 *
 * 【为什么存在】
 * - 每条 Pipeline 可以有多次执行（每次处理一份文档）
 * - 需要记录每次执行的状态、耗时、错误、产出的 chunk 数
 * - 用于任务监控与故障排查
 *
 * 【数据库表】t_ingestion_task
 *
 * 【关键字段】
 * - pipelineId：用的哪条 Pipeline
 * - sourceType / sourceLocation：数据源（file / url / feishu / s3）
 * - status：pending / running / completed / failed
 * - chunkCount：产出的切片数
 * - logsJson / metadataJson：JSONB 存储的日志与元数据
 *
 * 【被谁引用】
 * - IngestionTaskMapper（CRUD）
 * - IngestionService（任务调度与状态更新）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_ingestion_task")
public class IngestionTaskDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 使用的 Pipeline ID
     */
    private String pipelineId;

    /**
     * 数据源类型：file / url / feishu / s3
     */
    private String sourceType;

    /**
     * 数据源位置（文件路径或 URL）
     */
    private String sourceLocation;

    /**
     * 源文件名
     */
    private String sourceFileName;

    /**
     * 任务状态：pending / running / completed / failed
     */
    private String status;

    /**
     * 产出的切片数量
     */
    private Integer chunkCount;

    /**
     * 错误详情
     */
    private String errorMessage;

    /**
     * 执行日志 JSON（每个节点的耗时、输出等）
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String logsJson;

    /**
     * 任务元数据 JSON（解析器名、MIME、页数等）
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadataJson;

    /**
     * 开始时间
     */
    private Date startedAt;

    /**
     * 完成时间
     */
    private Date completedAt;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}