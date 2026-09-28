package com.bitselect.agent.rag.constant;

/**
 * 【文件用途】RAG 系统常量类：集中定义意图识别阈值、Prompt 模板路径、上下文格式化路径。
 *
 * 【为什么存在】
 * - 意图过滤阈值、意图数量上限、Prompt 路径都是"全局唯一的约定"
 * - 集中一处避免散落的魔法数字与硬编码路径
 * - 修改 Prompt 路径时只改一处，不用全项目搜索
 *
 * 【被谁引用】
 * - DefaultIntentClassifier（INTENT_CLASSIFIER_PROMPT_PATH）
 * - IntentResolver（INTENT_MIN_SCORE / MAX_INTENT_COUNT）
 * - RetrievalEngine（CONTEXT_FORMAT_PATH / MULTI_CHANNEL_KEY）
 * - ConversationTitleGenerator（CONVERSATION_TITLE_PROMPT_PATH）
 * - QueryRewriteService（QUERY_REWRITE_AND_SPLIT_PROMPT_PATH）
 * - IntentGuidanceService（GUIDANCE_PROMPT_PATH / GUIDANCE_AMBIGUITY_CHECK_PROMPT_PATH）
 * - 引用规则装配（ANSWER_CITATION_RULES_PROMPT_PATH）
 * - MCP 参数提取（MCP_PARAMETER_EXTRACT_PROMPT_PATH 等）
 */
public class RAGConstant {

    /**
     * 意图识别最低分数阈值。
     * 低于这个分数就当成"聊偏了"，不参与 RAG 检索流程。
     */
    public static final double INTENT_MIN_SCORE = 0.35;

    /**
     * 单次查询最多参与的意图数量上限。
     * 防止拉取过多 Collection 导致性能问题。
     */
    public static final int MAX_INTENT_COUNT = 3;

    /**
     * 多通道检索占位符键。
     * 当没有意图识别结果时，使用此键作为 intentChunks Map 的占位符。
     * 实际处理时只使用 Map 的 values，不关心具体的 key 值。
     */
    public static final String MULTI_CHANNEL_KEY = "multi_channel";

    /**
     * 意图识别提示词模板路径（串行模式）。
     */
    public static final String INTENT_CLASSIFIER_PROMPT_PATH = "prompt/intent-classifier.st";

    /**
     * 引导式问答提示词模板路径。
     */
    public static final String GUIDANCE_PROMPT_PATH = "prompt/guidance-prompt.st";

    /**
     * 歧义确认提示词模板路径。
     */
    public static final String GUIDANCE_AMBIGUITY_CHECK_PROMPT_PATH = "prompt/guidance-ambiguity-check.st";

    /**
     * 查询改写 + 多问句拆分提示词模板路径。
     */
    public static final String QUERY_REWRITE_AND_SPLIT_PROMPT_PATH = "prompt/user-question-rewrite.st";

    /**
     * 会话标题生成提示词模板路径。
     * 通过 {title_max_chars} 与 {question} 控制标题长度与输入问题。
     */
    public static final String CONVERSATION_TITLE_PROMPT_PATH = "prompt/conversation-title.st";

    /**
     * 知识资料回答的行内引用规则。
     * 仅在 rag.citation.enabled=true 且存在知识库上下文时，由 Prompt 编排层追加。
     */
    public static final String ANSWER_CITATION_RULES_PROMPT_PATH = "prompt/answer-citation-rules.st";

    /**
     * MCP 工具参数提取提示词模板路径。
     */
    public static final String MCP_PARAMETER_EXTRACT_PROMPT_PATH = "prompt/mcp-parameter-extract.st";

    /**
     * MCP 工具参数提取用户消息提示词模板路径。
     */
    public static final String MCP_PARAMETER_EXTRACT_USER_PROMPT_PATH = "prompt/mcp-parameter-extract-user.st";

    /**
     * 上下文格式化模板文件路径。
     * 包含所有上下文格式化所需的 section，通过 --- section: name --- 分隔，
     * 使用 PromptTemplateLoader.renderSection(path, section, slots) 渲染。
     */
    public static final String CONTEXT_FORMAT_PATH = "prompt/context-format.st";
}