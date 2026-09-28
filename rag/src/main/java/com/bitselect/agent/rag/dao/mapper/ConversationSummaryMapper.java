package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.ConversationSummaryDO;

/**
 * 【文件用途】会话摘要 Mapper：ConversationSummaryDO 的 CRUD。
 *
 * 【被谁引用】ConversationMessageService、ConversationGroupService
 */
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummaryDO> {
}