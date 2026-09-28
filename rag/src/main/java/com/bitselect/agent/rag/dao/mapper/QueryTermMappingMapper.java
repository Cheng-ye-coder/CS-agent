package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.QueryTermMappingDO;

/**
 * 【文件用途】查询词映射 Mapper：QueryTermMappingDO 的 CRUD。
 *
 * 【被谁引用】QueryTermMappingAdminService、QueryTermMappingCacheManager
 */
public interface QueryTermMappingMapper extends BaseMapper<QueryTermMappingDO> {
}