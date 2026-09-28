package com.bitselect.agent.core.parser;

import com.bitselect.agent.core.parser.model.ParsedDocument;
import com.bitselect.agent.core.parser.registry.ParseProfile;

import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】文档解析器统一接口：所有解析器（Markdown / CSV / Tika / Excel / MinerU / 图片）都实现它。
 *
 * 【为什么存在】
 * - 上游只需要一个统一契约：给我字节和 MIME，返回 ParsedDocument
 * - 需要"自我声明"机制：每个解析器告诉注册表"我能处理哪些 (MIME × 档位)"
 * - 需要类型标识：ParserType 让日志和元数据能记录"这篇文档是谁解析的"
 *
 * 【关键设计】
 * - 核心方法 parseStructured()：返回 Block 列表，而不是字符串——保留结构供下游分块器处理
 * - supportedMimeTypes() 是"认领清单"：启动期由 ParserRegistry 建表，键冲突即启动失败
 * - MIME 一律小写；支持 type/* 通配；精确键优先于通配键
 * - 未在请求档位注册时回落到 FAST 档，故只在该档位有专属实现时才需声明（如 Excel 的 FIDELITY 档）
 *
 * 【被谁引用】
 * - ParserRegistry（启动时收集所有实现）
 * - 各解析器实现类
 */
public interface DocumentParser {

    /**
     * 【方法用途】解析器类型标识，取值见 ParserType。
     */
    String getParserType();

    /**
     * 【方法用途】结构化解析：产出有序的 Block 列表（章节、段落、表格、图片等）。
     *
     * mimeType 与 options 可为空：
     * - mimeType 用于解析器内部判断子格式
     * - options 携带 sourceFile 等元数据
     */
    ParsedDocument parseStructured(byte[] content, String mimeType, Map<String, Object> options);

    /**
     * 【方法用途】认领清单：档位 → 该档位下认领的 MIME 集合，不得为空。
     *
     * MIME 一律小写；支持 type/* 通配，精确键优先于通配键。
     * 未在请求档位注册时回落到全局兜底档 FAST。
     */
    Map<ParseProfile, Set<String>> supportedMimeTypes();
}