

package com.bitselect.agent.rag.service.impl;

import com.bitselect.agent.framework.convention.ChatMessage;
import com.bitselect.agent.framework.convention.ChatRequest;
import com.bitselect.agent.framework.trace.RagTraceNode;
import com.bitselect.agent.infra.chat.LLMService;
import com.bitselect.agent.infra.enums.Tier;
import com.bitselect.agent.rag.config.MemoryProperties;
import com.bitselect.agent.rag.core.prompt.PromptTemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bitselect.agent.rag.constant.RAGConstant.CONVERSATION_TITLE_PROMPT_PATH;

/**
 * 会话标题生成器
 * <p>
 * 拆为独立 bean 是为了让 Spring AOP 的 {@link RagTraceNode} 拦截生效：
 * 同类 self-call 不会触发 proxy，所以原 ConversationServiceImpl 内部直接调
 * private 方法时，标题生成的 LLM 调用无法挂在 trace 节点下，会变成孤立的 root 节点
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationTitleGenerator {

    private final MemoryProperties memoryProperties;
    private final PromptTemplateLoader promptTemplateLoader;
    private final LLMService llmService;

    @RagTraceNode(name = "conversation-title-gen", type = "TITLE_GEN")
    public String generate(String question) {
        int maxLen = memoryProperties.getTitleMaxLength();
        if (maxLen <= 0) {
            maxLen = 30;
        }
        String prompt = promptTemplateLoader.render(
                CONVERSATION_TITLE_PROMPT_PATH,
                Map.of(
                        "title_max_chars", String.valueOf(maxLen),
                        "question", question
                )
        );

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(ChatMessage.user(prompt)))
                    .temperature(0.7D)
                    .topP(0.3D)
                    .thinking(false)
                    .build();
            return llmService.chat(request, Tier.FAST);
        } catch (Exception ex) {
            log.warn("生成会话标题失败", ex);
            return "新对话";
        }
    }
}
