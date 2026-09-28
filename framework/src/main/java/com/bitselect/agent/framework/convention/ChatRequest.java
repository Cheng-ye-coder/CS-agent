

package com.bitselect.agent.framework.convention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用大模型请求对象
 *
 * <p>
 * 用于封装一次完整对话所需的所有上下文与控制参数，作为「统一入参」传给
 * 各种不同厂商 / 协议的大模型接口（如 Ollama、百炼、OpenAI 等），
 * 方便在适配层做统一转换
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    /**
     * 完整消息列表
     */
    @Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 采样温度参数，取值通常为 0～2
     */
    private Double temperature;

    /**
     * nucleus sampling（Top-P）参数
     */
    private Double topP;

    /**
     * Top-K 采样参数
     */
    private Integer topK;

    /**
     * 限制模型本次回答最多生成的 token 数量
     */
    private Integer maxTokens;

    /**
     * 可选：是否启用「思考模式」开关
     */
    private Boolean thinking;

    /**
     * 可选：是否启用工具调用（Tool Calling / Function Calling）
     */
    private Boolean enableTools;
}