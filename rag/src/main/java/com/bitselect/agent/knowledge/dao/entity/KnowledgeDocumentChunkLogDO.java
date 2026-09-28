

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
 * 知识库文档分块日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("t_knowledge_document_chunk_log")
public class KnowledgeDocumentChunkLogDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String docId;

    private String status;

    private String processMode;

    private String parseProfile;

    private String pipelineId;

    private Long extractDuration;

    private Long chunkDuration;

    private Long embedDuration;

    private Long persistDuration;

    private Long totalDuration;

    private Integer chunkCount;

    private String errorMessage;

    private Date startTime;

    private Date endTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}