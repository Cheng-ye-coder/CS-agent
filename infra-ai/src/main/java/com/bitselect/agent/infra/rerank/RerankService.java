

package com.bitselect.agent.infra.rerank;

import com.bitselect.agent.framework.convention.RetrievedChunk;

import java.util.List;

/**
 * Rerank 服务：对向量检索出来的一批候选文档进行精排，
 * 按“和 query 的相关度”重新排序，并只返回前 topN 条
 */
public interface RerankService {

    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN);
}