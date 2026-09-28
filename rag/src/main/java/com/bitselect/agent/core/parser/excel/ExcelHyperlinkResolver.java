package com.bitselect.agent.core.parser.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;

/**
 * 【文件用途】Excel 超链接解析器：把 cell 内的超链接包装成 markdown 内联形式。
 *
 * 【为什么存在】
 * - Excel 的链接文字与底层 URL 是分离的：URL 存在 cell metadata 里
 * - 只读可见文字会丢掉 URL；只读 URL 会丢掉作者写的说明文字
 * - 必须显式读出 hyperlink 并拼接成 [text](url)，让下游 LLM 能同时看到
 * - MinerU 等 OCR / 版面识别工具无法拿到此元数据——这是 Excel 必须走 POI 的核心原因
 *
 * 【被谁引用】
 * - ExcelTableNormalizer.readGrid（读每个 cell 后调用）
 */
public final class ExcelHyperlinkResolver {

    private ExcelHyperlinkResolver() {
    }

    /**
     * 【方法用途】包装 cell 文字为 markdown 内联超链接形式。
     *
     * @param cellText cell 的可见文字（已格式化）
     * @param cell     cell 实例，用于查询 hyperlink；可空
     * @return 有非空超链接则返回 [text](url)，否则原样返回 cellText
     */
    public static String wrap(String cellText, Cell cell) {
        if (cell == null) {
            return cellText == null ? "" : cellText;
        }
        Hyperlink hyperlink = cell.getHyperlink();
        if (hyperlink == null) {
            return cellText == null ? "" : cellText;
        }
        String url = hyperlink.getAddress();
        if (url == null || url.isBlank()) {
            return cellText == null ? "" : cellText;
        }
        String visible = (cellText == null || cellText.isEmpty()) ? hyperlink.getLabel() : cellText;
        if (visible == null || visible.isEmpty()) {
            visible = url;
        }
        return "[" + visible + "](" + url + ")";
    }
}