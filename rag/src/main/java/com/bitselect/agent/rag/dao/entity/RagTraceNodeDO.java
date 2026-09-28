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
 * 【文件用途】RAG Trace 节点记录：一次请求中的单个执行阶段。
 *
 * 【为什么存在】
 * - 一次请求有多个节点（改写 / 检索 / 重排 / LLM 调用 / 工具调用）
 * - 通过 parentNodeId 构成树形结构，前端据此渲染链路树
 * - 节点级耗时用于定位性能瓶颈
 *
 * 【数据库表】t_rag_trace_node
 *
 * 【被谁引用】RagTraceNodeMapper、RagTraceRecordService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_rag_trace_node")
public class RagTraceNodeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String traceId;

    private String nodeId;

    private String parentNodeId;

    private Integer depth;

    /**
     * 节点类型：REWRITE / RETRIEVE / LLM / TOOL 等
     */
    private String nodeType;

    private String nodeName;

    private String className;

    private String methodName;

    /**
     * 状态：RUNNING / SUCCESS / ERROR
     */
    private String status;

    private String errorMessage;

    private Date startTime;
    private Date endTime;
    private Long durationMs;

    /**
     * 预留扩展字段（JSON 字符串）
     */
    private String extraData;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}