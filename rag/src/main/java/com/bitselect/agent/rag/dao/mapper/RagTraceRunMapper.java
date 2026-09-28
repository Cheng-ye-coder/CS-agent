package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.RagTraceRunDO;

/**
 * 【文件用途】RAG Trace 运行记录 Mapper：RagTraceRunDO 的 CRUD。
 *
 * 【被谁引用】RagTraceRecordService、RagTraceQueryService
 */
public interface RagTraceRunMapper extends BaseMapper<RagTraceRunDO> {
}