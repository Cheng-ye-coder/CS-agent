

package com.bitselect.agent.infra.chat;

import com.bitselect.agent.framework.trace.RagTraceNode;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 把 awaitFirstPacket 拆为独立 bean，便于 AOP 采集 TTFT trace
 */
@Component
public class LlmFirstPacketProbe {

    @RagTraceNode(name = "llm-first-packet", type = "LLM_TTFT")
    public ProbeStreamBridge.ProbeResult awaitFirstPacket(ProbeStreamBridge bridge,
                                                          long timeout,
                                                          TimeUnit unit) throws InterruptedException {
        return bridge.awaitFirstPacket(timeout, unit);
    }
}