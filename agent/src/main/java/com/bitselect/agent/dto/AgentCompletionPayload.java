

package com.bitselect.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * finish / cancel 事件载荷，durationMs 与落库同源
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentCompletionPayload(String messageId, String title, String messageStatus, Long durationMs) {
}
