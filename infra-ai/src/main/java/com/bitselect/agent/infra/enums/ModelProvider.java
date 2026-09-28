

package com.bitselect.agent.infra.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模型提供商枚举
 * 统一管理提供商名称，避免散落的字符串常量
 */
@Getter
@RequiredArgsConstructor
public enum ModelProvider {

    OLLAMA("ollama"),
    BAI_LIAN("bailian"),
    SILICON_FLOW("siliconflow"),
    AI_HUB_MIX("aihubmix"),
    DEEP_SEEK("deepseek"),
    NOOP("noop");

    private final String id;

    public boolean matches(String provider) {
        return provider != null && provider.equalsIgnoreCase(id);
    }
}