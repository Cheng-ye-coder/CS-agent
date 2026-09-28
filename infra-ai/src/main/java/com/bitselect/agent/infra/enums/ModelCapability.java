

package com.bitselect.agent.infra.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 模型能力枚举类
 * 定义了AI模型支持的各种能力类型
 */
@Getter
@RequiredArgsConstructor
public enum ModelCapability {

    /**
     * 聊天对话能力
     */
    CHAT("Chat"),

    /**
     * 向量嵌入能力
     */
    EMBEDDING("Embedding"),

    /**
     * 重排序能力
     */
    RERANK("Rerank");

    /**
     * 能力的显示名称
     */
    private final String displayName;
}