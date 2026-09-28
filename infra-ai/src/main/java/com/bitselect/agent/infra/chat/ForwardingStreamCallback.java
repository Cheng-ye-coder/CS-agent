

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.convention.GroundingChunk;
import com.bitselect.agent.framework.convention.SourceRef;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 透传式 StreamCallback 装饰器
 */
public abstract class ForwardingStreamCallback implements StreamCallback {

    private final StreamCallback delegate;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final AtomicBoolean firstContentSeen = new AtomicBoolean(false);

    protected ForwardingStreamCallback(StreamCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public final void onContent(String content) {
        if (firstContentSeen.compareAndSet(false, true)) {
            try {
                onFirstContent();
            } catch (Throwable ex) {
                // 钩子异常不能影响正常推流
            }
        }
        delegate.onContent(content);
    }

    @Override
    public final void onThinking(String content) {
        delegate.onThinking(content);
    }

    @Override
    public final void onReplyToMessageId(String messageId) {
        delegate.onReplyToMessageId(messageId);
    }

    @Override
    public final void onSources(List<SourceRef> sources) {
        delegate.onSources(sources);
    }

    @Override
    public final void onGroundingChunks(List<GroundingChunk> chunks) {
        delegate.onGroundingChunks(chunks);
    }

    protected void onFirstContent() {
    }

    @Override
    public final void onComplete() {
        try {
            delegate.onComplete();
        } finally {
            finishOnce(true, null);
        }
    }

    @Override
    public final void onError(Throwable error) {
        try {
            delegate.onError(error);
        } finally {
            finishOnce(false, error);
        }
    }

    protected final void finishExternally(boolean success, Throwable error) {
        finishOnce(success, error);
    }

    private void finishOnce(boolean success, Throwable error) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        onFinish(success, error);
    }

    protected abstract void onFinish(boolean success, Throwable error);
}