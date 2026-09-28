package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.ConversationDO;

/**
 * 【文件用途】会话 Mapper：ConversationDO 的 CRUD。
 *
 * 【被谁引用】ConversationService、ConversationGroupService
 */
public interface ConversationMapper extends BaseMapper<ConversationDO> {
}