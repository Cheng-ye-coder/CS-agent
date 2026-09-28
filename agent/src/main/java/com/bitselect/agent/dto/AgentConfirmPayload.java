

package com.bitselect.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * SSE confirm 事件载荷，挂起也是本段 run 的收口，续跑那段独立计时
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentConfirmPayload(String messageId, String title, List<AgentConfirmCall> calls, Long durationMs) {
}
