package com.bitselect.agent.core.parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】文档解析后的文本规范化工具。
 *
 * 【为什么存在】
 * - 各解析器输出的文本常带 BOM、行尾空格、连续空行等噪音
 * - 需要统一清洗后再进入分块阶段，否则会污染向量文本
 * - 清洗逻辑集中一处，避免各解析器重复实现
 *
 * 【具体清洗】
 * 1. 剥除 BOM（\uFEFF）——Windows 记事本保存 UTF-8 时常带
 * 2. 去行尾空格与制表符——某些解析器会保留原始缩进残留
 * 3. 连续三个以上空行压成两个——避免段落之间有过大的空隙
 * 4. 去首尾空白
 *
 * 【被谁引用】TikaDocumentParser（解析后调用）。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextCleanupUtil {

    /**
     * 【方法用途】依次剥 BOM、去行尾空格与制表符、连续三个以上空行压成两个、去首尾空白。
     */
    public static String cleanup(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\uFEFF", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}