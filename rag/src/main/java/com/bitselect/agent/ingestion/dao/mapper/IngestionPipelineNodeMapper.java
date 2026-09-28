package com.bitselect.agent.ingestion.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.ingestion.dao.entity.IngestionPipelineNodeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 【文件用途】Pipeline 节点 Mapper：提供节点的 CRUD 操作。
 *
 * 【为什么存在】
 * - 继承 BaseMapper 获得基础 CRUD
 * - 额外提供一个"物理删除"方法：MyBatis-Plus 的 @TableLogic 默认只做逻辑删除，
 *   但 Pipeline 更新时全量替换节点，需要真正清空旧节点
 *
 * 【关键方法】
 * - physicalDeleteByPipelineId：物理删除某个 Pipeline 下的所有节点
 *   用于"更新 Pipeline 时先清空旧节点再插入新节点"的场景
 *
 * 【被谁引用】
 * - PipelineService（保存 Pipeline 时先清空旧节点）
 */
public interface IngestionPipelineNodeMapper extends BaseMapper<IngestionPipelineNodeDO> {

    /**
     * 【方法用途】物理删除某个 Pipeline 下的所有节点。
     *
     * 注意：这是物理删除（DELETE FROM），不走 @TableLogic 的逻辑删除。
     * 用于 Pipeline 更新时的全量替换：先物理删除旧节点，再插入新节点。
     */
    @Delete("DELETE FROM t_ingestion_pipeline_node WHERE pipeline_id = #{pipelineId}")
    int physicalDeleteByPipelineId(@Param("pipelineId") String pipelineId);
}