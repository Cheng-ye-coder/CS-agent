

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.convention.ChatRequest;
import com.bitselect.agent.infra.model.ModelTarget;

/**
 * 聊天客户端接口
 * 定义了与AI模型进行对话的核心方法
 */
public interface ChatClient {

    String provider();

    String chat(ChatRequest request, ModelTarget target);

    StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback, ModelTarget target);
}