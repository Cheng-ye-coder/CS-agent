

package com.bitselect.agent.knowledge.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档处理状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum DocumentStatus {

    PENDING("pending"),
    RUNNING("running"),
    FAILED("failed"),
    SUCCESS("success");

    private final String code;
}