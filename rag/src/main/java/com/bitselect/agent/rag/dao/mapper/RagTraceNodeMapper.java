package com.bitselect.agent.rag.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.rag.dao.entity.RagTraceNodeDO;

/**
 * 【文件用途】RAG Trace 节点 Mapper：RagTraceNodeDO 的 CRUD。
 *
 * 【被谁引用】RagTraceRecordService、RagTraceQueryService
 */
public interface RagTraceNodeMapper extends BaseMapper<RagTraceNodeDO> {
}