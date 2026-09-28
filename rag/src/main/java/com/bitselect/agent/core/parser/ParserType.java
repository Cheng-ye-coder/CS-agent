package com.bitselect.agent.core.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 【文件用途】文档解析器类型枚举：标记"这个解析器是干什么的"。
 *
 * 【为什么存在】
 * - 每个 DocumentParser 实现需要自报类型（getParserType() 的返回值）
 * - 元数据里会记录"这篇文档是用哪个解析器解析的"，用于排障与审计
 * - 前端可能根据解析器类型展示不同的信息（如 Excel 显示"表格"图标）
 *
 * 【被谁引用】
 * - DocumentParser.getParserType() 返回
 * - 各实现类（TIKA / MARKDOWN / CSV / EXCEL_POI / MINERU / IMAGE）
 *
 * 【扩展点】新增解析器时在此追加枚举项。
 */
@Getter
@RequiredArgsConstructor
public enum ParserType {

    /**
     * Tika 解析器：纯文本类格式的兜底（HTML / JSON / XML / RTF / text/*）
     */
    TIKA("Tika"),

    /**
     * Markdown 解析器：commonmark-java，支持 GFM 表格与内嵌 HTML
     */
    MARKDOWN("Markdown"),

    /**
     * Apache POI Excel 解析器：合并单元格 / 多行表头 / 超链接
     */
    EXCEL_POI("ExcelPoi"),

    /**
     * CSV 解析器：自动探测字符集 + RFC4180 解析，产出单张 key-val 表格
     */
    CSV("Csv"),

    /**
     * MinerU SaaS 解析器：PDF / Word / PPT / Excel 的复杂版面解析
     */
    MINERU("MinerU"),

    /**
     * 图片解析器：VLM 图生文 + 原图入库
     */
    IMAGE("Image");

    /**
     * 解析器类型名称（用于元数据记录与展示）
     */
    private final String type;
}