

package com.bitselect.agent.infra.rerank;

import com.bitselect.agent.framework.convention.RetrievedChunk;
import com.bitselect.agent.infra.model.ModelTarget;

import java.util.List;

/**
 * Rerank客户端接口
 * 用于对检索到的文档片段进行重新排序，以提高检索结果的相关性
 */
public interface RerankClient {

    String provider();

    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN, ModelTarget target);
}