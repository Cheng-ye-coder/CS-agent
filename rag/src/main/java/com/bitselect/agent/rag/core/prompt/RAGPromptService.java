package com.bitselect.agent.rag.core.prompt;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.framework.convention.ChatMessage;
import com.bitselect.agent.rag.config.RAGConfigProperties;
import com.bitselect.agent.rag.core.intent.IntentNode;
import com.bitselect.agent.rag.core.intent.NodeScore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bitselect.agent.rag.constant.RAGConstant.ANSWER_CITATION_RULES_PROMPT_PATH;
import static com.bitselect.agent.rag.constant.RAGConstant.CONTEXT_FORMAT_PATH;

/**
 * 【文件用途】RAG Prompt 编排服务：根据检索场景选择模板，构造最终发送给 LLM 的消息序列。
 *
 * 【为什么存在】
 * - 场景（KB_ONLY / MCP_ONLY / MIXED）决定系统提示词模板
 * - 单意图可覆写节点级 promptTemplate
 * - citationEligible=false 时跳过引用规则拼接
 *
 * 【被谁引用】StreamChatPipeline / KnowledgeSearchFacade
 */
@Service
@RequiredArgsConstructor
public class RAGPromptService {

    private final PromptTemplateLoader templateLoader;
    private final AgentPromptResolver agentPromptResolver;
    private final RAGConfigProperties ragConfigProperties;

    /**
     * 【方法用途】生成系统提示词，并对模板格式做清理。
     */
    public String buildSystemPrompt(PromptContext context) {
        return buildSystemPrompt(context, true);
    }

    /**
     * citationEligible=false 时无条件跳过引用规则拼接。
     */
    private String buildSystemPrompt(PromptContext context, boolean citationEligible) {
        PromptBuildPlan plan = plan(context);
        String template = StrUtil.isNotBlank(plan.getBaseTemplate())
                ? plan.getBaseTemplate()
                : defaultTemplate(plan.getScene());
        String systemPrompt = StrUtil.isBlank(template) ? "" : PromptTemplateUtils.cleanupPrompt(template);
        if (!citationEligible || !context.hasKb() || !Boolean.TRUE.equals(ragConfigProperties.getCitationEnabled())) {
            return systemPrompt;
        }

        String citationRules = PromptTemplateUtils.cleanupPrompt(
                templateLoader.load(ANSWER_CITATION_RULES_PROMPT_PATH));
        if (StrUtil.isBlank(systemPrompt)) {
            return citationRules;
        }
        if (StrUtil.isBlank(citationRules)) {
            return systemPrompt;
        }
        return systemPrompt + "\n\n" + citationRules;
    }

    /**
     * 【方法用途】构造发送给 LLM 的完整消息列表（system + evidence + history + user）。
     */
    public List<ChatMessage> buildStructuredMessages(PromptContext context,
                                                     List<ChatMessage> history,
                                                     String question,
                                                     List<String> subQuestions) {
        return buildStructuredMessages(context, history, question, subQuestions, true);
    }

    /**
     * citationEligible 透传给系统提示词分支，false 表示调用方不具备角标渲染能力。
     */
    public List<ChatMessage> buildStructuredMessages(PromptContext context,
                                                     List<ChatMessage> history,
                                                     String question,
                                                     List<String> subQuestions,
                                                     boolean citationEligible) {
        List<ChatMessage> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(context, citationEligible);
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(ChatMessage.system(systemPrompt));
        }

        if (CollUtil.isNotEmpty(history)) {
            messages.addAll(history);
        }

        String evidenceBody = buildEvidenceBody(context);
        String userQuestion = buildUserQuestion(question, subQuestions);
        String userContent = mergeEvidenceAndQuestion(evidenceBody, userQuestion);
        if (StrUtil.isNotBlank(userContent)) {
            messages.add(ChatMessage.user(userContent));
        }

        return messages;
    }

    private PromptPlan planPrompt(List<NodeScore> intents, Set<String> eligibleIntentIds) {
        List<NodeScore> safeIntents = intents == null ? Collections.emptyList() : intents;
        Map<String, NodeScore> eligibleById = new LinkedHashMap<>();
        for (NodeScore intent : safeIntents) {
            if (intent == null || intent.getNode() == null) {
                continue;
            }
            String intentId = intent.getNode().getId();
            if (!eligibleIntentIds.contains(intentId)) {
                continue;
            }
            eligibleById.putIfAbsent(intentId, intent);
        }
        List<NodeScore> eligibleIntents = new ArrayList<>(eligibleById.values());

        if (eligibleIntents.isEmpty()) {
            return new PromptPlan(Collections.emptyList(), null);
        }

        if (eligibleIntents.size() == 1) {
            IntentNode only = eligibleIntents.get(0).getNode();
            String tpl = StrUtil.emptyIfNull(only.getPromptTemplate()).trim();
            if (StrUtil.isNotBlank(tpl)) {
                return new PromptPlan(eligibleIntents, tpl);
            }
        }
        return new PromptPlan(eligibleIntents, null);
    }

    private PromptBuildPlan plan(PromptContext context) {
        if (context.hasMcp() && !context.hasKb()) {
            return planMcpOnly(context);
        }
        if (!context.hasMcp() && context.hasKb()) {
            return planKbOnly(context);
        }
        if (context.hasMcp() && context.hasKb()) {
            return planMixed(context);
        }
        throw new IllegalStateException("PromptContext requires MCP or KB context.");
    }

    private PromptBuildPlan planKbOnly(PromptContext context) {
        PromptPlan plan = planPrompt(context.getKbIntents(), context.getEligibleIntentIds());
        return PromptBuildPlan.builder()
                .scene(PromptScene.KB_ONLY)
                .baseTemplate(plan.getBaseTemplate())
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptBuildPlan planMcpOnly(PromptContext context) {
        List<NodeScore> intents = context.getMcpIntents();
        String baseTemplate = null;
        if (CollUtil.isNotEmpty(intents) && intents.size() == 1) {
            IntentNode node = intents.get(0).getNode();
            String tpl = StrUtil.emptyIfNull(node.getPromptTemplate()).trim();
            if (StrUtil.isNotBlank(tpl)) {
                baseTemplate = tpl;
            }
        }

        return PromptBuildPlan.builder()
                .scene(PromptScene.MCP_ONLY)
                .baseTemplate(baseTemplate)
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private PromptBuildPlan planMixed(PromptContext context) {
        return PromptBuildPlan.builder()
                .scene(PromptScene.MIXED)
                .mcpContext(context.getMcpContext())
                .kbContext(context.getKbContext())
                .question(context.getQuestion())
                .build();
    }

    private String defaultTemplate(PromptScene scene) {
        return switch (scene) {
            case KB_ONLY -> agentPromptResolver.resolve(AgentPromptSlot.KB_ANSWER);
            case MCP_ONLY -> agentPromptResolver.resolve(AgentPromptSlot.MCP_ANSWER);
            case MIXED -> agentPromptResolver.resolve(AgentPromptSlot.MIXED_ANSWER);
            case EMPTY -> "";
        };
    }

    private String buildUserQuestion(String question, List<String> subQuestions) {
        if (CollUtil.isNotEmpty(subQuestions) && subQuestions.size() > 1) {
            String numbered = IntStream.range(0, subQuestions.size())
                    .mapToObj(i -> (i + 1) + ". " + subQuestions.get(i))
                    .collect(Collectors.joining("\n"));
            return renderSection("multi-questions", Map.of("questions", numbered));
        }
        if (StrUtil.isBlank(question)) {
            return "";
        }
        return renderSection("single-question", Map.of("question", question));
    }

    private String mergeEvidenceAndQuestion(String evidenceBody, String question) {
        if (StrUtil.isBlank(evidenceBody)) {
            return question;
        }
        if (StrUtil.isBlank(question)) {
            return evidenceBody;
        }
        return evidenceBody + "\n\n" + question;
    }

    private String buildEvidenceBody(PromptContext context) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(context.getMcpContext())) {
            sb.append(renderSection("mcp-evidence", Map.of("body", context.getMcpContext().trim())));
        }
        if (StrUtil.isNotBlank(context.getKbContext())) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(renderSection("kb-evidence", Map.of("body", context.getKbContext().trim())));
        }
        return sb.toString().trim();
    }

    private String renderSection(String section, Map<String, String> slots) {
        return templateLoader.renderSection(CONTEXT_FORMAT_PATH, section, slots);
    }
}