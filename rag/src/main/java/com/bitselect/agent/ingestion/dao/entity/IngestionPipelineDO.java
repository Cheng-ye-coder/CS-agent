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
 * 【文件用途】数据摄入管道实体：定义一条可编排的文档处理流水线。
 *
 * 【为什么存在】
 * - 复杂文档（PDF / Word / 复杂 Excel）需要多步骤处理：抓取 → 解析 → 清洗 → 分块 → 富化 → 向量化
 * - 每个步骤是一个节点，节点串起来构成 Pipeline
 * - 用户可以选择"用哪个 Pipeline 处理这份文档"（通过 processMode=pipeline + pipelineId）
 *
 * 【数据库表】t_ingestion_pipeline
 *
 * 【被谁引用】
 * - IngestionPipelineMapper（CRUD）
 * - PipelineService（加载 Pipeline 定义）
 * - IngestionPipelineNodeDO（一对多）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_ingestion_pipeline")
public class IngestionPipelineDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * Pipeline 名称（用户可见）
     */
    private String name;

    /**
     * 描述（说明这条 Pipeline 干什么）
     */
    private String description;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}