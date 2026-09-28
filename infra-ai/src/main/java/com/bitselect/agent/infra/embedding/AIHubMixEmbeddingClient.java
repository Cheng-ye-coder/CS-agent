

package com.bitselect.agent.infra.embedding;

import com.bitselect.agent.infra.enums.ModelProvider;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

@Service
public class AIHubMixEmbeddingClient extends AbstractOpenAIStyleEmbeddingClient {

    public AIHubMixEmbeddingClient(OkHttpClient syncHttpClient) {
        super(syncHttpClient);
    }

    @Override
    public String provider() {
        return ModelProvider.AI_HUB_MIX.getId();
    }

    @Override
    protected int maxBatchSize() {
        return 32;
    }
}