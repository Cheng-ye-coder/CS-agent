

package com.bitselect.agent.knowledge.dao.entity;

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
 * RAG 知识库文档分块表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_knowledge_chunk")
public class KnowledgeChunkDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String kbId;

    private String docId;

    private Integer chunkIndex;

    private String content;

    private String contentHash;

    private Integer charCount;

    private Integer tokenCount;

    /**
     * 向量文本：章节路径 + 正文
     * <p>
     * 落库不是为了展示——它是重建向量的唯一正确来源
     */
    private String embeddingText;

    private Integer enabled;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}