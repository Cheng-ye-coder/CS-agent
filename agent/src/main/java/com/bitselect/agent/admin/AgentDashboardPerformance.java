

package com.bitselect.agent.admin;

import com.bitselect.agent.admin.controller.vo.DashboardPerformance;

import java.util.List;

/**
 * 仅暴露聚合值，不返回聊天正文、工具参数、记忆内容或用户标识
 */
public record AgentDashboardPerformance(
        String window,
        long updatedAt,
        Replies replies,
        Tools tools,
        Confirmations confirmations,
        Memory memory
) implements DashboardPerformance {

    @Override
    public String getEngine() {
        return "agent";
    }

    /**
     * directReplies/singleToolReplies/multiToolReplies 三档相加等于 withBlocks，不是 total
     * previousTotal 只统计条数不看轨迹，deltaPct 与概览四个 KPI 走同一个环比口径
     */
    public record Replies(long total, long previousTotal, Double deltaPct, long normal, long interrupted,
                          long awaitingConfirm, long unknown, long withBlocks, long directReplies,
                          long singleToolReplies, long multiToolReplies) {
    }

    public record Tools(long total, long done, long failed, long interrupted, long other,
                        Double callsPerRecordedReply, long knowledgeSearchCalls, List<ToolCount> topTools) {
    }

    /**
     * done + failed 未必等于 count：还有中断与未知状态，成功率的分母是 count
     * lastCallAt 是最近一次调用所在回复的创建时间，不是调用本身的时刻
     */
    public record ToolCount(String name, String displayName, long count, long done, long failed,
                            Double successRate, Long lastCallAt) {
    }

    public record Confirmations(long total, long approved, long denied, long pending, long expired, long other,
                                Double approvalRate, List<ConfirmToolCount> topTools) {
    }

    /**
     * 一张卡整体裁决，卡内每个调用共用同一个结果，所以 calls 之和不小于卡片总数
     */
    public record ConfirmToolCount(String name, String displayName, long calls, long approved, long denied) {
    }

    /**
     * contextChars 是上下文字符数而非 token，两者量纲不同，展示措辞不可混用
     * 字符数只统计 compactionsWithChars 这批事件，求平均时分母也只能用它
     */
    public record Memory(long compactions, long compactionsWithChars, Double contextReductionPct,
                         long contextCharsBefore, long contextCharsAfter, long activeMemories,
                         long addedMemories, long invalidatedMemories) {
    }
}
