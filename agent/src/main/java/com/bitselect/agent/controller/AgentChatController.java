

package com.bitselect.agent.controller;

import com.bitselect.agent.config.AgentProperties;
import com.bitselect.agent.config.ConditionalOnAgentEngine;
import com.bitselect.agent.controller.request.ConfirmRequest;
import com.bitselect.agent.service.AgentChatService;
import com.bitselect.agent.framework.convention.Result;
import com.bitselect.agent.framework.validation.ChatQuestion;
import com.bitselect.agent.framework.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 对话入口，仅 ragent.engine.type=agent 时注册；RAG v3 接口不受影响
 */
@RestController
@ConditionalOnAgentEngine
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;
    private final AgentProperties agentProperties;

    @GetMapping(value = "/agent/v1/chat", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chat(@RequestParam @ChatQuestion String question,
                           @RequestParam(required = false) String conversationId) {
        SseEmitter emitter = new SseEmitter(agentProperties.getSseTimeoutMs());
        agentChatService.streamChat(question, conversationId, emitter);
        return emitter;
    }

    @PostMapping(value = "/agent/v1/chat/confirm", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter confirm(@RequestBody ConfirmRequest requestParam) {
        SseEmitter emitter = new SseEmitter(agentProperties.getSseTimeoutMs());
        agentChatService.confirmPendingTool(requestParam.conversationId(), requestParam.messageId(),
                requestParam.approved(), emitter);
        return emitter;
    }

    @PostMapping("/agent/v1/stop")
    public Result<Void> stop(@RequestParam String taskId) {
        agentChatService.stopTask(taskId);
        return Results.success();
    }
}
