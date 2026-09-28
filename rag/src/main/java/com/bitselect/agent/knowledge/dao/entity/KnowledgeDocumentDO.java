

package com.bitselect.agent.knowledge.dao.entity;

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
 * 知识库文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_knowledge_document")
public class KnowledgeDocumentDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String kbId;

    private String docName;

    private String sourceType;

    private String sourceLocation;

    private Integer scheduleEnabled;

    private String scheduleCron;

    private Integer enabled;

    private Integer chunkCount;

    private String fileUrl;

    private String fileType;

    private String mimeType;

    private Long fileSize;

    private String processMode;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String ingestionSpec;

    private String pipelineId;

    private String status;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}