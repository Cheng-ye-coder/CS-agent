package com.bitselect.agent.rag.core.prompt;

import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【文件用途】Prompt 模板工具：占位符填充、多空行清理、section 解析。
 *
 * 【为什么存在】
 * - 模板渲染需要统一的"填充 + 清理"逻辑
 * - context-format.st 用 --- section: name --- 分隔多个片段，需要统一解析
 *
 * 【被谁引用】PromptTemplateLoader / AgentPromptResolver / RAGPromptService
 */
public final class PromptTemplateUtils {

    private static final Pattern MULTI_BLANK_LINES = Pattern.compile("(\\n){3,}");
    private static final Pattern SECTION_HEADER = Pattern.compile("^---\\s*section:\\s*(\\S+)\\s*---$", Pattern.MULTILINE);

    public static String cleanupPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return MULTI_BLANK_LINES.matcher(prompt).replaceAll("\n\n").trim();
    }

    public static String fillSlots(String template, Map<String, String> slots) {
        if (template == null) {
            return "";
        }
        if (slots == null || slots.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : slots.entrySet()) {
            String value = StrUtil.emptyIfNull(entry.getValue());
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    /**
     * 【方法用途】将包含 --- section: name --- 分隔符的模板文件解析为 name → content 映射。
     */
    public static Map<String, String> parseSections(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        if (StrUtil.isBlank(content)) {
            return sections;
        }
        Matcher matcher = SECTION_HEADER.matcher(content);
        int lastStart = -1;
        String lastName = null;
        while (matcher.find()) {
            if (lastName != null) {
                sections.put(lastName, trimSection(content.substring(lastStart, matcher.start())));
            }
            lastName = matcher.group(1);
            lastStart = matcher.end();
        }
        if (lastName != null) {
            sections.put(lastName, trimSection(content.substring(lastStart)));
        }
        return sections;
    }

    private static String trimSection(String section) {
        if (section.startsWith("\n")) {
            section = section.substring(1);
        }
        return section.stripTrailing();
    }
}