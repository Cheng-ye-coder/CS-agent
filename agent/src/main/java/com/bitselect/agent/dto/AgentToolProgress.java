

package com.bitselect.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE tool 事件载荷，status 与落库块的状态同名同值，result 和 ok 仅终态时携带
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentToolProgress(String toolCallId, String name, String displayName, String status,
                                String result, Boolean ok, String at, String batchId, Integer callIndex,
                                Long startedAt, Long endedAt, Long durationMs, String durationSource) {

    /**
     * 从块投影，SSE 与落库同源，避免刷新前后出现两个不同的数
     */
    public static AgentToolProgress of(AgentBlock block) {
        return new AgentToolProgress(block.getToolCallId(), block.getName(), block.getDisplayName(),
                block.getStatus(), block.getResult(), ok(block.getStatus()), block.getAt(), block.getBatchId(),
                block.getCallIndex(), block.getStartedAt(), block.getEndedAt(), block.getDurationMs(),
                block.getDurationSource());
    }

    /**
     * 只在终态表态，非终态返回 null
     */
    private static Boolean ok(String status) {
        return switch (status) {
            case "done" -> Boolean.TRUE;
            case "failed", "denied", "interrupted" -> Boolean.FALSE;
            default -> null;
        };
    }
}
