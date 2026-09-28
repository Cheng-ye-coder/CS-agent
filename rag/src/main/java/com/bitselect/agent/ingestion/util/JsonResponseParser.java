package com.bitselect.agent.ingestion.util;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.infra.util.LLMResponseCleaner;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】JSON 响应解析器：从 LLM 返回的文本中提取 JSON 结构。
 *
 * 【为什么存在】
 * - LLM 经常在 JSON 外包裹说明文字或 markdown 代码围栏，不能直接反序列化
 * - 需要剥离围栏、定位 JSON 起始/结束位置，再解析
 * - 集中处理"宽容解析"，避免每个调用点重复写清理逻辑
 *
 * 【关键设计】
 * - 先剥 markdown 围栏（用 LLMResponseCleaner），再定位第一个 { 或 [ 到最后一个 } 或 ]
 * - 解析失败返回空集合（宽容策略），不抛异常
 * - 支持对象与数组两种目标类型
 *
 * 【被谁引用】
 * - ingestion 的 PromptNode / 各类 LLM 加工节点
 * - 任何需要从 LLM 输出提取 JSON 的场景
 */
public final class JsonResponseParser {

    private static final Gson GSON = new Gson();

    private JsonResponseParser() {
    }

    /**
     * 【方法用途】解析为字符串列表：期望 LLM 返回 JSON 数组。
     */
    public static List<String> parseStringList(String raw) {
        JsonElement element = parseJsonElement(raw);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        return GSON.fromJson(element, List.class);
    }

    /**
     * 【方法用途】解析为 Map：期望 LLM 返回 JSON 对象。
     */
    public static Map<String, Object> parseObject(String raw) {
        JsonElement element = parseJsonElement(raw);
        if (element == null || !element.isJsonObject()) {
            return Collections.emptyMap();
        }
        return GSON.fromJson(element, LinkedHashMap.class);
    }

    /**
     * 【方法用途】宽容解析：剥围栏 → 定位 JSON 边界 → 解析。
     */
    private static JsonElement parseJsonElement(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String cleaned = LLMResponseCleaner.stripMarkdownCodeFence(raw);
        String trimmed = extractJsonBody(cleaned);
        try {
            return JsonParser.parseString(trimmed);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * 【方法用途】定位 JSON 主体：从第一个 { 或 [ 到最后一个 } 或 ]。
     *
     * LLM 常在 JSON 前后追加"这是结果：""以上就是分析"之类的话，
     * 截取边界后再解析能避免 JsonSyntaxException。
     */
    private static String extractJsonBody(String raw) {
        int objStart = raw.indexOf('{');
        int arrStart = raw.indexOf('[');
        int start;
        if (objStart < 0) {
            start = arrStart;
        } else if (arrStart < 0) {
            start = objStart;
        } else {
            start = Math.min(objStart, arrStart);
        }
        if (start < 0) {
            return raw;
        }
        int objEnd = raw.lastIndexOf('}');
        int arrEnd = raw.lastIndexOf(']');
        int end = Math.max(objEnd, arrEnd);
        if (end < 0 || end <= start) {
            return raw.substring(start);
        }
        return raw.substring(start, end + 1);
    }
}