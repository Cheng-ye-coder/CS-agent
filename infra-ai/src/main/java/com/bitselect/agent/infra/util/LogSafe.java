

package com.bitselect.agent.infra.util;

import lombok.NoArgsConstructor;

/**
 * 日志安全工具类
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class LogSafe {

    private static final int DEFAULT_MAX = 500;

    public static String preview(String raw) {
        return preview(raw, DEFAULT_MAX);
    }

    public static String preview(String raw, int max) {
        if (raw == null) {
            return null;
        }
        if (raw.length() <= max) {
            return raw;
        }
        return raw.substring(0, max) + "...(truncated, total " + raw.length() + " chars)";
    }
}