

package com.bitselect.agent.rag.core.memory;

import com.bitselect.agent.framework.convention.ChatMessage;

public interface ConversationMemorySummaryService {

    void compressIfNeeded(String conversationId, String userId, ChatMessage message);

    ChatMessage loadLatestSummary(String conversationId, String userId);

    ChatMessage decorateIfNeeded(ChatMessage summary);
}
