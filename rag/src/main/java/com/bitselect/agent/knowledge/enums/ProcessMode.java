


// 定义了文档入库的两种处理模式（chunk，pipeline）
// 前者是默认的分块策略模式，按照固定规则切块的简单流程
// 后者pipeline管道模式，文档经过可编排的多节点流程（解析-清晰-分块-富化-向量化）
// 比如有售后政策这种复杂结构的文档，就需要后者

package com.bitselect.agent.knowledge.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档处理模式枚举
 */
@Getter
@RequiredArgsConstructor
public enum ProcessMode {

    CHUNK("chunk"),
    PIPELINE("pipeline");

    private final String value;

    public static ProcessMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        for (ProcessMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        return null;
    }

// 这是企业级代码里常见的"双转换"模式：

// 方法	适用场景	行为
// fromValue	可选参数、配置解析、宽容场景	转换失败返回 null
// normalize	必填参数、API 入口、严格场景	转换失败抛异常

    public static ProcessMode normalize(String value) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("处理模式不能为空");
        }
        ProcessMode result = fromValue(value);
        if (result == null) {
            throw new IllegalArgumentException("不支持的处理模式: " + value);
        }
        return result;
    }
}