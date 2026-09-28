package com.bitselect.agent.ingestion.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.ingestion.dao.entity.IngestionPipelineDO;

/**
 * 【文件用途】Pipeline 实体 Mapper：提供 Pipeline 的 CRUD 操作。
 *
 * 【为什么存在】
 * - 继承 MyBatis-Plus 的 BaseMapper，自动获得 selectById / selectList / insert / update / delete 等方法
 * - 无需手写 SQL（除非有复杂查询需求）
 *
 * 【被谁引用】
 * - PipelineService（加载 / 保存 Pipeline 定义）
 */
public interface IngestionPipelineMapper extends BaseMapper<IngestionPipelineDO> {
}