

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.convention.ChatRequest;
import com.bitselect.agent.infra.enums.Tier;

/**
 * 通用大语言模型（LLM）访问接口
 */
public interface LLMService {

    String chat(ChatRequest request);

    String chat(ChatRequest request, Tier tier);

    String chat(ChatRequest request, Tier tier, String preferredModelId);

    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback);
}