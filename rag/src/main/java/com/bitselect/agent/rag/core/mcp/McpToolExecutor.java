

package com.bitselect.agent.rag.core.mcp;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;

/**
 * MCP 工具执行器接口
 */
public interface McpToolExecutor {

    /**
     * 获取工具定义
     *
     * @return 工具元信息（使用官方 SDK 的 Tool）
     */
    Tool getToolDefinition();

    /**
     * 执行工具调用，{@code meta} 承载登录身份这类传输层信息，不进工具入参（见 {@link McpCallMeta}）
     * <p>
     * 抽象的是带 meta 这条而不是单参那条：实现类必须当场决定身份怎么办。反过来写则漏实现它照样能编译，
     * 身份被 default 悄悄丢掉，不抛异常也不打日志，只在用户那侧表现成查不到自己的数据
     *
     * @param parameters 调用参数
     * @param meta       传输层元信息，没有身份可带时传空表
     * @return 工具调用结果（使用官方 SDK 的 CallToolResult）
     */
    CallToolResult execute(Map<String, Object> parameters, Map<String, Object> meta);

    /**
     * 不需要身份的调用方入口，转调时给空表
     */
    default CallToolResult execute(Map<String, Object> parameters) {
        return execute(parameters, Map.of());
    }

    /**
     * 工具 ID（快捷方法）
     */
    default String getToolId() {
        return getToolDefinition().name();
    }
}
