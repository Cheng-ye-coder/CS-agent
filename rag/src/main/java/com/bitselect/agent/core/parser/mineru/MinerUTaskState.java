package com.bitselect.agent.core.parser.mineru;

/**
 * 【文件用途】MinerU 任务状态枚举：把 SaaS 返回的状态字符串映射成标准枚举。
 *
 * 【为什么存在】
 * - MinerU 的状态字段有多个别名（done / success / succeeded / completed 都表示完成）
 * - 需要一个权威枚举供轮询器判断"完成 / 失败 / 继续轮询"
 * - 把别名归一化集中一处，避免调用方到处写字符串比较
 *
 * 【状态映射】
 * - waiting-file / pending / running / converting / queueing → RUNNING
 * - done / success / succeeded / completed → DONE
 * - failed / fail / error → FAILED
 * - 无法识别 → UNKNOWN（视为继续轮询）
 *
 * 【被谁引用】MinerUStatus、MinerUPollingExecutor。
 */
public enum MinerUTaskState {
    RUNNING,
    DONE,
    FAILED,
    UNKNOWN;

    /**
     * 【方法用途】从 MinerU 字段值映射到枚举，无法识别返回 UNKNOWN。
     */
    public static MinerUTaskState parse(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        return switch (raw.toLowerCase()) {
            case "done", "success", "succeeded", "completed" -> DONE;
            case "failed", "fail", "error" -> FAILED;
            case "waiting-file", "pending", "running", "converting", "queueing", "queue" -> RUNNING;
            default -> UNKNOWN;
        };
    }
}