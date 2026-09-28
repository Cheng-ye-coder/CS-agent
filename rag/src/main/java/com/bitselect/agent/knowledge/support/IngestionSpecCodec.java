package com.bitselect.agent.knowledge.support;

import com.bitselect.agent.core.chunk.model.ChunkBudget;
import com.bitselect.agent.core.ingest.IngestionSpec;
import com.bitselect.agent.core.parser.registry.ParseProfile;
import com.bitselect.agent.framework.exception.ClientException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 【文件用途】文档级摄取配置的读写、校验与归一化：全系统单点。
 *
 * 【为什么存在】
 * - 前端提交的是扁平 JSON，库里存的是嵌套结构，需要双向翻译
 * - 取值范围由 ChunkBudget 与 ParseProfile 的构造期保证，此处只把构造异常翻译成用户可读的报错
 * - 缺失字段一律回落 IngestionSpec.defaults()，全系统只此一份默认值
 * - 前端提交什么形状都在此收敛成规整 JSON 落库，读路径不必再探测
 *
 * 【关键设计】
 * - 线路上的键名常量（KEY_MAX_CHARS 等）与 IngestionSpecSchemaProvider 共用，
 *   避免两端各写一份字符串
 * - WHOLE_DOCUMENT_SENTINEL = -1：线路上的"整篇不分块"约定
 *   与领域内部的 Integer.MAX_VALUE 的翻译只发生在本类
 * - read() 必须走 SpecWire 而非直接反序列化领域对象：
 *   库里的 maxChars 是 -1，撞上 ChunkBudget 的"必须 > 0"校验会被静默换成默认配置
 *
 * 【被谁引用】
 * - KnowledgeDocumentService（上传时归一化配置）
 * - IngestionNode（读取配置）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionSpecCodec {

    public static final String KEY_MAX_CHARS = "maxChars";
    public static final String KEY_OVERLAP_CHARS = "overlapChars";
    public static final String KEY_ROWS_PER_CHUNK = "rowsPerChunk";
    public static final String KEY_TOLERANCE_FACTOR = "toleranceFactor";

    private static final String KEY_PARSE_PROFILE = "parseProfile";

    /**
     * 不分块哨兵：沿用前端既有约定的 -1。
     */
    public static final int WHOLE_DOCUMENT_SENTINEL = -1;

    private final ObjectMapper objectMapper;

    /**
     * 【方法用途】读：库里的 JSON → 配置对象，空值或损坏一律回落默认。
     */
    public IngestionSpec read(String json) {
        if (!StringUtils.hasText(json)) {
            return IngestionSpec.defaults();
        }
        try {
            return objectMapper.readValue(json, SpecWire.class).toDomain();
        } catch (Exception e) {
            log.warn("摄取配置解析失败，回落默认配置：{}", json, e);
            return IngestionSpec.defaults();
        }
    }

    /**
     * 【方法用途】写：配置对象 → 落库 JSON。
     */
    public String write(IngestionSpec spec) {
        try {
            return objectMapper.writeValueAsString(SpecWire.of(spec == null ? IngestionSpec.defaults() : spec));
        } catch (Exception e) {
            throw new ClientException("摄取配置序列化失败");
        }
    }

    /**
     * 【方法用途】校验并归一化前端提交的配置 JSON，返回落库用的规整 JSON。
     *
     * @param rawJson 前端提交的原始 JSON，可空（空表示全默认）
     * @return 规整后的 JSON，入参为空时返回 null
     */
    public String normalize(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return null;
        }
        Map<String, Object> raw;
        try {
            raw = objectMapper.readValue(rawJson.trim(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ClientException("摄取配置 JSON 格式不合法");
        }
        try {
            return write(fromMap(raw));
        } catch (IllegalArgumentException e) {
            throw new ClientException("摄取配置不合法：" + e.getMessage());
        }
    }

    private IngestionSpec fromMap(Map<String, Object> raw) {
        return IngestionSpec.of(ParseProfile.from(readString(raw, KEY_PARSE_PROFILE)),
                toBudget(readInt(raw, KEY_MAX_CHARS), readInt(raw, KEY_OVERLAP_CHARS),
                        readInt(raw, KEY_ROWS_PER_CHUNK), readInt(raw, KEY_TOLERANCE_FACTOR)));
    }

    /**
     * 四个整数 → 分块预算：哨兵翻译与缺失回落只有这一份。
     */
    private static ChunkBudget toBudget(Integer maxChars, Integer overlap, Integer rows, Integer tolerance) {
        if (maxChars != null && maxChars == WHOLE_DOCUMENT_SENTINEL) {
            return ChunkBudget.wholeDocument();
        }
        ChunkBudget defaults = ChunkBudget.defaults();
        int budget = maxChars != null && maxChars > 0 ? maxChars : defaults.maxChars();
        return new ChunkBudget(
                budget,
                overlap != null && overlap >= 0 ? overlap : ChunkBudget.defaultOverlapFor(budget),
                rows != null && rows > 0 ? rows : defaults.rowsPerChunk(),
                tolerance != null && tolerance > 0 ? tolerance : defaults.toleranceFactor());
    }

    private static Integer readInt(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String readString(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 摄取配置的线路形状：与 IngestionSpec 同构，唯一区别是整篇不分块用
     * WHOLE_DOCUMENT_SENTINEL 表达，而非领域内部的 Integer.MAX_VALUE。
     */
    private record SpecWire(int version, ParseProfile parseProfile, BudgetWire budget) {

        static SpecWire of(IngestionSpec spec) {
            ChunkBudget budget = spec.budget();
            return new SpecWire(spec.version(), spec.parseProfile(), budget.isWholeDocument()
                    ? new BudgetWire(WHOLE_DOCUMENT_SENTINEL, 0, WHOLE_DOCUMENT_SENTINEL, budget.toleranceFactor())
                    : new BudgetWire(budget.maxChars(), budget.overlapChars(),
                    budget.rowsPerChunk(), budget.toleranceFactor()));
        }

        IngestionSpec toDomain() {
            return new IngestionSpec(
                    version > 0 ? version : IngestionSpec.CURRENT_VERSION,
                    parseProfile,
                    budget == null
                            ? ChunkBudget.defaults()
                            : toBudget(budget.maxChars(), budget.overlapChars(),
                            budget.rowsPerChunk(), budget.toleranceFactor()));
        }
    }

    /**
     * 分块预算的线路形状：字段允许缺失，缺失一律由 toBudget 回落默认。
     */
    private record BudgetWire(Integer maxChars, Integer overlapChars, Integer rowsPerChunk, Integer toleranceFactor) {
    }
}