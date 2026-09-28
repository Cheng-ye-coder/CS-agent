package com.bitselect.agent.rag.util;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 【文件用途】展示类型：文件在界面上的短标签（pdf / docx / xlsx / markdown 等）。
 *
 * 【为什么存在】
 * - 前端需要展示"这个文件是什么类型"的图标与标签
 * - 展示语义的权威源是上传时的扩展名，字节语义的权威源是 MIME 探测，两者不得互相导出
 *   （.md 按字节探测通常只得到 text/plain，若展示标签从 MIME 反推，"规范.md" 会显示成 txt）
 * - 此枚举永不参与路由，解析器与分块器的选择只认 MIME
 *
 * 【关键设计】
 * - of(filename, mimeType)：扩展名优先，无扩展名时看 MIME，都认不出返回 OTHER
 * - isTabular()：表格类判定，前后端共用这一份
 * - extensions()：反查该类型的所有扩展名
 * - 兜底一律 OTHER，不回落成原始 MIME 字符串（落库列是 VARCHAR(16)）
 *
 * 【被谁引用】
 * - IngestionSpecSchemaProvider（判断哪些 MIME 是表格类）
 * - 前端展示层（图标选择）
 */
public enum DisplayType {

    PDF("pdf"),
    WORD("doc"),
    WORD_X("docx"),
    PPT("ppt"),
    PPT_X("pptx"),
    EXCEL("xls"),
    EXCEL_X("xlsx"),
    CSV("csv"),
    MARKDOWN("markdown"),
    TEXT("txt"),
    HTML("html"),
    JSON("json"),
    XML("xml"),
    RTF("rtf"),
    PNG("png"),
    JPG("jpg"),
    SVG("svg"),

    /**
     * 兜底：认不出一律 other，不回落成原始 MIME 字符串（落库列是 VARCHAR(16)）。
     */
    OTHER("other");

    /**
     * 扩展名 → 展示类型，权威映射。
     */
    private static final Map<String, DisplayType> BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", PDF),
            Map.entry("doc", WORD),
            Map.entry("docx", WORD_X),
            Map.entry("ppt", PPT),
            Map.entry("pptx", PPT_X),
            Map.entry("xls", EXCEL),
            Map.entry("xlsx", EXCEL_X),
            Map.entry("csv", CSV),
            Map.entry("md", MARKDOWN),
            Map.entry("markdown", MARKDOWN),
            Map.entry("txt", TEXT),
            Map.entry("text", TEXT),
            Map.entry("html", HTML),
            Map.entry("htm", HTML),
            Map.entry("json", JSON),
            Map.entry("xml", XML),
            Map.entry("rtf", RTF),
            Map.entry("png", PNG),
            Map.entry("jpg", JPG),
            Map.entry("jpeg", JPG),
            Map.entry("svg", SVG)
    );

    /**
     * MIME → 展示类型，仅在没有扩展名时兜底使用。
     */
    private static final Map<String, DisplayType> BY_MIME = Map.ofEntries(
            Map.entry("application/pdf", PDF),
            Map.entry("application/x-pdf", PDF),
            Map.entry("application/msword", WORD),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", WORD_X),
            Map.entry("application/vnd.ms-powerpoint", PPT),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", PPT_X),
            Map.entry("application/vnd.ms-excel", EXCEL),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", EXCEL_X),
            Map.entry("text/csv", CSV),
            Map.entry("text/markdown", MARKDOWN),
            Map.entry("text/x-markdown", MARKDOWN),
            Map.entry("text/plain", TEXT),
            Map.entry("text/html", HTML),
            Map.entry("application/json", JSON),
            Map.entry("application/xml", XML),
            Map.entry("application/rtf", RTF),
            Map.entry("image/png", PNG),
            Map.entry("image/jpeg", JPG),
            Map.entry("image/svg+xml", SVG)
    );

    /**
     * 表格类判定：前后端共用这一份，不各自维护扩展名列表。
     */
    private static final Set<DisplayType> TABULAR = Set.of(EXCEL, EXCEL_X, CSV);

    private final String code;

    DisplayType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 【方法用途】是否表格类文件。
     */
    public boolean isTabular() {
        return TABULAR.contains(this);
    }

    /**
     * 【方法用途】该展示类型的全部扩展名，反查 BY_EXTENSION 下发给前端做界面判定。
     */
    public Set<String> extensions() {
        Set<String> result = new TreeSet<>();
        BY_EXTENSION.forEach((extension, type) -> {
            if (type == this) {
                result.add(extension);
            }
        });
        return result;
    }

    /**
     * 【方法用途】唯一产生点：扩展名优先，无扩展名时才看 MIME，都认不出就是 OTHER。
     */
    public static DisplayType of(String filename, String mimeType) {
        DisplayType byExtension = BY_EXTENSION.get(extractExtension(filename));
        if (byExtension != null) {
            return byExtension;
        }
        return BY_MIME.getOrDefault(normalizeMime(mimeType), OTHER);
    }

    /**
     * 【方法用途】反解落库值，未知一律 OTHER。
     */
    public static DisplayType from(String code) {
        if (code == null || code.isBlank()) {
            return OTHER;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (DisplayType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return OTHER;
    }

    private static String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String name = filename.trim();
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (separator >= 0 && separator + 1 < name.length()) {
            name = name.substring(separator + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "";
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        int semicolon = normalized.indexOf(';');
        return semicolon >= 0 ? normalized.substring(0, semicolon).trim() : normalized;
    }
}