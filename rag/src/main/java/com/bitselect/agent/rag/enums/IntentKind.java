package com.bitselect.agent.rag.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 【文件用途】意图类型枚举：区分用户意图的不同类型。
 *
 * 【为什么存在】
 * - 意图树叶子节点分三类：KB（走 RAG 检索）、MCP（走工具调用）、SYSTEM（走系统交互）
 * - 路由层据此决定走哪条链路
 * - code 用于数据库存储（t_intent_node.kind 存 int）
 *
 * 【被谁引用】IntentNode / IntentNodeMapper / DefaultIntentClassifier
 */
@Getter
@RequiredArgsConstructor
public enum IntentKind {

    /**
     * 知识库类，走 RAG。
     */
    KB(0),

    /**
     * 系统交互类，如欢迎语、介绍自己。
     */
    SYSTEM(1),

    /**
     * MCP，实时数据交互。
     */
    MCP(2);

    private final int code;

    /**
     * 【方法用途】根据编码获取对应的意图类型。
     */
    public static IntentKind fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (IntentKind e : values()) {
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