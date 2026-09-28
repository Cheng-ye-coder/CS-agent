

package com.bitselect.agent.controller;

import com.bitselect.agent.config.AgentProperties;
import com.bitselect.agent.config.ConditionalOnAgentEngine;
import com.bitselect.agent.controller.vo.AgentMetaVO;
import com.bitselect.agent.tool.AgentToolCatalog;
import com.bitselect.agent.framework.convention.Result;
import com.bitselect.agent.framework.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 引擎探活与身份，前端进入聊天页先拉一次点亮徽标，绝不带密钥
 */
@RestController
@ConditionalOnAgentEngine
@RequiredArgsConstructor
public class AgentMetaController {

    private final AgentProperties agentProperties;
    private final AgentToolCatalog toolCatalog;

    @GetMapping("/agent/v1/meta")
    public Result<AgentMetaVO> meta() {
        boolean mcpConfigured = toolCatalog.mcpToolCount() > 0;
        // 能力清单随实况增删，否则会与 mcpConfigured 各说各话，前端只能自己对齐
        List<String> capabilities = new ArrayList<>(List.of("react", "knowledge-base"));
        if (mcpConfigured) {
            capabilities.add("mcp-tools");
        }
        return Results.success(new AgentMetaVO(
                "AgentScope ReAct",
                agentProperties.getChat().getModel(),
                agentProperties.getMaxIters(),
                List.copyOf(capabilities),
                mcpConfigured ? "native + mcp" : "native",
                mcpConfigured));
    }
}
