package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.IntentNodeDO;

/**
 * 【文件用途】意图树节点 Mapper：IntentNodeDO 的 CRUD。
 *
 * 【被谁引用】IntentNodeRegistry（启动时加载意图树）
 */
public interface IntentNodeMapper extends BaseMapper<IntentNodeDO> {
}