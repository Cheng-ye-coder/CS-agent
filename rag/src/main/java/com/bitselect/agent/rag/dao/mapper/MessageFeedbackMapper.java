package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.MessageFeedbackDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 【文件用途】消息反馈 Mapper：MessageFeedbackDO 的 CRUD + 两个自定义 upsert。
 *
 * 【为什么有自定义 SQL】
 * - 反馈的唯一约束是 (message_id, user_id)，需要 UPSERT 语义：
 *   存在则更新、不存在则插入
 * - 用 PostgreSQL 的 ON CONFLICT ... DO UPDATE 实现原子 upsert
 * - update_time 作为版本号：只有当新事件的时间更新时才覆盖，避免多节点并行消费乱序
 *
 * 【两个方法】
 * - upsertActiveFeedback：写入有效反馈（deleted=0）
 * - upsertCancelledFeedback：写入取消反馈（deleted=1），不存在时创建占位，保证重复取消幂等
 *
 * 【被谁引用】MessageFeedbackService
 */
public interface MessageFeedbackMapper extends BaseMapper<MessageFeedbackDO> {

    /**
     * 【方法用途】写入有效反馈：存在则更新，不存在则插入。
     */
    @Insert("""
            INSERT INTO t_message_feedback
                (id, message_id, conversation_id, user_id, vote, reason, comment, create_time, update_time, deleted)
            VALUES
                (#{feedback.id}, #{feedback.messageId}, #{feedback.conversationId}, #{feedback.userId},
                 #{feedback.vote}, #{feedback.reason}, #{feedback.comment},
                 #{feedback.createTime}, #{feedback.updateTime}, 0)
            ON CONFLICT (message_id, user_id) DO UPDATE SET
                conversation_id = EXCLUDED.conversation_id,
                vote = EXCLUDED.vote,
                reason = EXCLUDED.reason,
                comment = EXCLUDED.comment,
                update_time = EXCLUDED.update_time,
                deleted = 0
            WHERE t_message_feedback.update_time < EXCLUDED.update_time
            """)
    int upsertActiveFeedback(@Param("feedback") MessageFeedbackDO feedback);

    /**
     * 【方法用途】写入取消反馈：记录不存在时创建逻辑删除占位，保证重复取消幂等。
     */
    @Insert("""
            INSERT INTO t_message_feedback
                (id, message_id, conversation_id, user_id, vote, reason, comment, create_time, update_time, deleted)
            VALUES
                (#{feedback.id}, #{feedback.messageId}, #{feedback.conversationId}, #{feedback.userId},
                 0, NULL, NULL, #{feedback.createTime}, #{feedback.updateTime}, 1)
            ON CONFLICT (message_id, user_id) DO UPDATE SET
                update_time = EXCLUDED.update_time,
                deleted = 1
            WHERE t_message_feedback.update_time < EXCLUDED.update_time
            """)
    int upsertCancelledFeedback(@Param("feedback") MessageFeedbackDO feedback);
}