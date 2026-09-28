

package com.bitselect.agent.knowledge.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 知识库文档定时刷新执行记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_knowledge_document_schedule_exec")
public class KnowledgeDocumentScheduleExecDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String scheduleId;

    private String docId;

    private String kbId;

    private String status;

    private String message;

    private Date startTime;

    private Date endTime;

    private String fileName;

    private Long fileSize;

    private String contentHash;

    private String etag;

    private String lastModified;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}