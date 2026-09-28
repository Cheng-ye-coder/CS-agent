package com.bitselect.agent.rag.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 【文件用途】会话创建/更新业务对象：Service 层与持久层之间的传输对象。
 *
 * 【为什么存在】
 * - Controller 的 Request DTO 与数据库 DO 不应直接传递
 * - 用 BO 做中间层，隔离外部接口与内部存储的字段变化
 *
 * 【被谁引用】ConversationService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationCreateBO {

    /**
     * 会话 ID
     */
    private String conversationId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户问题（用于生成会话标题）
     */
    private String question;

    /**
     * 最后更新时间
     */
    private Date lastTime;
}