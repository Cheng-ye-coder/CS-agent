package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.ConversationMessageDO;

/**
 * 【文件用途】会话消息 Mapper：ConversationMessageDO 的 CRUD。
 *
 * 【被谁引用】ConversationMessageService、ConversationGroupService
 */
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageDO> {
}