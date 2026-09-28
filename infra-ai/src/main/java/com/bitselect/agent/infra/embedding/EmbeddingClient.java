

package com.bitselect.agent.infra.embedding;

import com.bitselect.agent.infra.model.ModelTarget;

import java.util.List;

/**
 * 文本嵌入客户端接口
 */
public interface EmbeddingClient {

    String provider();

    List<Float> embed(String text, ModelTarget target);

    List<List<Float>> embedBatch(List<String> texts, ModelTarget target);
}