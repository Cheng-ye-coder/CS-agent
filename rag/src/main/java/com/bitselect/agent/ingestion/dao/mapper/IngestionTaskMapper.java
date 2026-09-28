package com.bitselect.agent.ingestion.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.ingestion.dao.entity.IngestionTaskDO;

/**
 * 【文件用途】摄取任务 Mapper：提供任务记录的 CRUD 操作。
 *
 * 【为什么存在】
 * - 记录每次 Pipeline 执行的状态与结果
 * - 提供任务查询（按状态、按 Pipeline、按时间范围）
 *
 * 【被谁引用】
 * - IngestionService（任务调度、状态更新）
 * - 管理后台（任务列表查询）
 */
public interface IngestionTaskMapper extends BaseMapper<IngestionTaskDO> {
}