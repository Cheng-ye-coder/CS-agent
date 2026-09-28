

package com.bitselect.agent.mcp.config;

import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;

/**
 * MCP 工具注解预定义常量，读/写两种
 */
public final class McpToolAnnotations {

    /**
     * 只读工具：不改动任何数据，重复调用等价
     */
    public static final ToolAnnotations READ_ONLY = new ToolAnnotations(null, true, false, true, false, null);

    /**
     * 写工具：产生真实业务副作用，重复调用不等价
     */
    public static final ToolAnnotations WRITE = new ToolAnnotations(null, false, false, false, false, null);

    private McpToolAnnotations() {
    }
}
