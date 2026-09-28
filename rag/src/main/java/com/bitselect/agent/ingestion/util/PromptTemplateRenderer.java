package com.bitselect.agent.ingestion.util;

import java.util.Map;

/**
 * 【文件用途】提示词模板渲染器：把 {{key}} 占位符替换成变量值。
 *
 * 【为什么存在】
 * - Ingestion Pipeline 里的各类 Prompt 节点需要动态拼接用户输入、上下文、配置
 * - 用最简的 {{key}} 语法，不引入模板引擎依赖
 * - 渲染逻辑集中一处，方便后续统一优化（如支持条件、转义）
 *
 * 【语法】
 * - 模板：{{variableName}}
 * - 变量：Map<"variableName", value>
 * - 未提供的变量替换为空串（宽容策略）
 *
 * 【被谁引用】
 * - ingestion/prompt 包的模板加载器
 * - 各类 LLM 加工节点
 */
public final class PromptTemplateRenderer {

    private PromptTemplateRenderer() {
    }

    /**
     * 【方法用途】渲染模板，把 {{key}} 替换为变量值。
     *
     * @param template  模板文本
     * @param variables 变量映射
     * @return 渲染后的文本；模板为空时原样返回
     */
    public static String render(String template, Map<String, Object> variables) {
        if (template == null || template.isBlank()) {
            return template;
        }
        String out = template;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                out = out.replace(key, value);
            }
        }
        return out;
    }
}