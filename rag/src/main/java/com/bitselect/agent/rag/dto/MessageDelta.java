package com.bitselect.agent.rag.dto;

/**
 * 【文件用途】消息增量：SSE 流式输出中的一个内容片段。
 *
 * 【为什么存在】
 * - 流式回答需要逐 token 或逐块推送给前端
 * - 一个 delta 包含"类型 + 内容"两要素
 * - 类型用于区分"思考内容" / "正文内容" / 其他
 *
 * 【被谁引用】
 * - rag 的 SSE 流式输出
 * - 前端逐 delta 拼接显示
 */
public record MessageDelta(String type, String delta) {

    /**
     * 消息类型：正文内容。
     */
    public String type() {
        return type;
    }

    /**
     * 增量数据：本次推送的内容片段。
     */
    public String delta() {
        return delta;
    }
}