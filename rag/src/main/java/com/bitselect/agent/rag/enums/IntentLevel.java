package com.bitselect.agent.rag.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 【文件用途】意图层级枚举：意图树的三级结构。
 *
 * 【为什么存在】
 * - 意图树有三级：DOMAIN → CATEGORY → TOPIC
 * - 只有叶子节点参与意图打分，层级用于提示词语义组织
 * - code 用于数据库存储（t_intent_node.level 存 int）
 *
 * 【被谁引用】IntentNode / IntentTreeFactory
 */
@Getter
@RequiredArgsConstructor
public enum IntentLevel {

    /**
     * 顶层：集团信息化 / 业务系统 / 中间件环境信息。
     */
    DOMAIN(0),

    /**
     * 第二层：人事 / 行政 / OA系统 / Redis ...
     */
    CATEGORY(1),

    /**
     * 第三层：更具体的 Topic，如系统介绍 / 数据安全 / 架构设计。
     */
    TOPIC(2);

    private final int code;

    /**
     * 【方法用途】根据编码获取对应的意图层级。
     */
    public static IntentLevel fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IntentLevel e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name();
    }
}