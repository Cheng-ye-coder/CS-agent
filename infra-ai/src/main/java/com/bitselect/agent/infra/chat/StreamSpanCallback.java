

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.trace.RagStreamTraceSupport.StreamSpan;

/**
 * 把 StreamSpan 的 finish 桥接到 StreamCallback 的终态事件
 */
public final class StreamSpanCallback extends ForwardingStreamCallback {

    private final StreamSpan span;

    public StreamSpanCallback(StreamCallback delegate, StreamSpan span) {
        super(delegate);
        this.span = span;
    }

    @Override
    protected void onFinish(boolean success, Throwable error) {
        if (success) {
            span.finishSuccess();
        } else {
            span.finishError(error);
        }
    }

    public void onCancel() {
        span.finishCancelledIfRunning();
        finishExternally(false, null);
    }
}