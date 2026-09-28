package com.bitselect.agent.ingestion.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.ingestion.dao.entity.IngestionTaskNodeDO;

/**
 * 【文件用途】任务节点 Mapper：提供节点级执行记录的 CRUD 操作。
 *
 * 【为什么存在】
 * - 记录一次任务中每个节点的执行情况
 * - 支持按 taskId 查询该任务的所有节点记录，用于前端展示执行链路
 *
 * 【被谁引用】
 * - IngestionService（每个节点执行后写入记录）
 * - 管理后台（任务详情页展示节点链路）
 */
public interface IngestionTaskNodeMapper extends BaseMapper<IngestionTaskNodeDO> {
}