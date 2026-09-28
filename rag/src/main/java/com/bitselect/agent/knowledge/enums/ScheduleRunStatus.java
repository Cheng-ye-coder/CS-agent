

package com.bitselect.agent.knowledge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 定时任务执行状态
 */
@Getter
@RequiredArgsConstructor
public enum ScheduleRunStatus {

    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String code;
}