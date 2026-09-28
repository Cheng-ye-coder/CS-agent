

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.convention.GroundingChunk;
import com.bitselect.agent.framework.convention.SourceRef;

import java.util.List;

/**
 * 流式响应回调接口
 */
public interface StreamCallback {

    default void onReplyToMessageId(String messageId) {
    }

    void onContent(String content);

    default void onThinking(String content) {
    }

    default void onSources(List<SourceRef> sources) {
    }

    default void onGroundingChunks(List<GroundingChunk> chunks) {
    }

    void onComplete();

    void onError(Throwable error);
}