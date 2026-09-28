package com.bitselect.agent.core.parser.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * 【文件用途】解析档位枚举：用户在"成本"与"保真度"之间的选择。
 *
 * 【为什么存在】
 * - 同一份文档可以用不同成本解析：本地解析器（免费、慢）vs 外部 SaaS（收费、快）
 * - 与格式（MIME 唯一确定）是两个正交维度：格式决定"用什么解析器"，档位决定"哪一档解析器"
 * - 是"解析档位"这个选项能在界面上展示的基础
 *
 * 【关键设计】
 * - FAST 是全局兜底档：请求档位无匹配时自动回落到它，再不行显式报错
 * - 枚举名是引擎侧的词（FAST / FIDELITY），界面文案由 IngestionSpecSchemaProvider 单点下发
 * - 用 @JsonValue / @JsonCreator 让序列化为小写字符串（"fast" / "fidelity"）
 * - from() 采用"空值回落默认，非法值报错"策略：不静默兜底
 *
 * 【被谁引用】
 * - DocumentParser.supportedMimeTypes()（认领清单的键）
 * - ParserRegistry（建表与查找）
 * - IngestionSpec（落库配置的字段）
 */
public enum ParseProfile {

    /**
     * 快速档：本地解析器优先，零外部成本；也是全局兜底档。
     */
    FAST("fast"),

    /**
     * 保真档：付出外部服务成本换版面还原度，如 Excel 走 MinerU 版面解析。
     */
    FIDELITY("fidelity");

    private final String code;

    ParseProfile(String code) {
        this.code = code;
    }

    /**
     * 【方法用途】序列化为小写字符串（JSON / 数据库都存这个值）。
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 【方法用途】默认档：全局兜底用 FAST。
     */
    public static ParseProfile defaultProfile() {
        return FAST;
    }

    /**
     * 【方法用途】宽松解析：空值回落默认档，无法识别的取值直接报错而非静默兜底。
     */
    @JsonCreator
    public static ParseProfile from(String code) {
        if (code == null || code.isBlank()) {
            return defaultProfile();
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (ParseProfile profile : values()) {
            if (profile.code.equals(normalized)) {
                return profile;
            }
        }
        throw new IllegalArgumentException("未知解析档位：" + code);
    }
}