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
 * 【文件用途】RAG Trace 运行记录：一次完整请求的执行记录。
 *
 * 【为什么存在】
 * - 一次请求对应一条 Run，是链路追踪的根节点
 * - 记录整体状态、耗时、用户、会话，供管理后台查询与排障
 *
 * 【数据库表】t_rag_trace_run
 *
 * 【被谁引用】RagTraceRunMapper、RagTraceRecordService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_rag_trace_run")
public class RagTraceRunDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String traceId;

    private String traceName;

    private String entryMethod;

    private String conversationId;

    private String taskId;

    private String userId;

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