

package com.bitselect.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSE block 事件载荷：一段文本流完了，把服务端量到的起止交给前端
 * <p>
 * 不带正文，前端手里的增量已经是全文，再发一遍只是把同一段文字传两趟
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentTextBlockSeal(String kind, String at, Long startedAt, Long endedAt, Long durationMs) {

    /**
     * 直接从块投影：SSE 与落库两份要是各算一遍，刷新前后就成了两条不同的记录
     */
    public static AgentTextBlockSeal of(AgentBlock block) {
        return new AgentTextBlockSeal(block.getKind(), block.getAt(), block.getStartedAt(),
                block.getEndedAt(), block.getDurationMs());
    }
}
