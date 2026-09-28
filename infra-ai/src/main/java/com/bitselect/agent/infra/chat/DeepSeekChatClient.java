

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.convention.ChatRequest;
import com.bitselect.agent.framework.trace.RagTraceNode;
import com.bitselect.agent.infra.enums.ModelProvider;
import com.bitselect.agent.infra.model.ModelTarget;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DeepSeek 官方开放平台 ChatClient
 */
@Slf4j
@Service
public class DeepSeekChatClient extends AbstractOpenAIStyleChatClient {

    @Override
    public String provider() {
        return ModelProvider.DEEP_SEEK.getId();
    }

    @Override
    protected void customizeRequestBody(JsonObject body, ChatRequest request) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", Boolean.TRUE.equals(request.getThinking()) ? "enabled" : "disabled");
        body.add("thinking", thinking);
    }

    @Override
    @RagTraceNode(name = "deepseek-chat", type = "LLM_PROVIDER")
    public String chat(ChatRequest request, ModelTarget target) {
        return doChat(request, target);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target) {
        return doStreamChat(request, callback, target);
    }
}