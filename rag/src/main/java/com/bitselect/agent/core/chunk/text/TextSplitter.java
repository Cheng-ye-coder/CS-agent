package com.bitselect.agent.core.chunk.text;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 【文件用途】边界感知的文本切分工具：按预算切开长文本，切点落在自然边界而非下标处。
 *
 * 【为什么存在】
 * - 段落、代码块超预算时需要降级切分，直接 substring 会把句子、URL、数字腰斩
 * - 中文和英文的句末标点不同，需要分别识别
 * - 文档解析后常有软换行破坏词语/URL，需要归一化
 *
 * 【核心能力】
 * 1. split：边界回溯切分（换行 → 中文句末 → 英文句末）
 * 2. normalize：文本归一化（去 \r、修复断行 URL、合并 CJK 软换行）
 *
 * 【关键设计】
 * - 切点回溯距离不超过 overlap：避免相邻片高度重复
 * - 英文点号必须后接空白才算边界：否则会把 URL 的域名点切开
 * - 强制推进机制：回退过头时用 targetEnd 兜底，防止无限循环
 *
 * 【被谁引用】
 * - ParagraphChunker（切分段落）
 * - 任何需要"安全切长文本"的场景
 */
public final class TextSplitter {

    private TextSplitter() {
    }

    /**
     * 【方法用途】切分文本，空文本返回空列表。
     *
     * @param text         待切分文本
     * @param maxChars     单片最大字符数
     * @param overlapChars 相邻片重叠字符数，同时作为边界回溯的最大距离
     * @return 切分后的文本片列表（已归一化）
     */
    public static List<String> split(String text, int maxChars, int overlapChars) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = normalize(text);
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        int chunkSize = Math.max(1, maxChars);
        int overlap = chunkSize > 1 ? Math.min(Math.max(0, overlapChars), chunkSize - 1) : 0;
        int len = normalized.length();

        List<String> pieces = new ArrayList<>();
        int start = 0;
        int lastEnd = -1;
        while (start < len) {
            int targetEnd = Math.min(start + chunkSize, len);
            int end = adjustToBoundary(normalized, start, targetEnd, overlap);
            // 【关键】强制推进：回退过头会导致片段重复甚至停滞，用 targetEnd 兜底
            if (end <= start || end <= lastEnd) {
                end = targetEnd;
            }
            String piece = normalized.substring(start, end);
            if (StringUtils.hasText(piece.strip())) {
                pieces.add(piece);
            }
            lastEnd = end;
            if (end >= len) {
                break;
            }
            int nextStart = Math.max(0, end - overlap);
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }
        return pieces;
    }

    /**
     * 【方法用途】边界回溯：优先换行，其次中文句末标点，最后英文句末标点。
     *
     * 英文点号必须后接空白或结尾才算边界，否则会把 URL 的域名点切开。
     * 回溯距离不超过 overlap，避免相邻片高度重复。
     */
    private static int adjustToBoundary(String text, int start, int targetEnd, int overlap) {
        if (targetEnd <= start) {
            return targetEnd;
        }
        int maxLookback = Math.min(overlap, targetEnd - start);
        if (maxLookback <= 0) {
            return targetEnd;
        }

        // 第一优先：换行边界
        for (int i = 0; i <= maxLookback; i++) {
            int pos = targetEnd - i - 1;
            if (pos <= start) {
                break;
            }
            if (text.charAt(pos) == '\n') {
                return pos + 1;
            }
        }
        // 第二优先：中文句末标点
        for (int i = 0; i <= maxLookback; i++) {
            int pos = targetEnd - i - 1;
            if (pos <= start) {
                break;
            }
            char c = text.charAt(pos);
            if (c == '。' || c == '！' || c == '？') {
                return pos + 1;
            }
        }
        // 第三优先：英文句末标点（必须后接空白或结尾）
        for (int i = 0; i <= maxLookback; i++) {
            int pos = targetEnd - i - 1;
            if (pos <= start) {
                break;
            }
            char c = text.charAt(pos);
            if (c == '.' || c == '!' || c == '?') {
                int next = pos + 1;
                if (next >= text.length() || Character.isWhitespace(text.charAt(next))) {
                    return next;
                }
            }
        }
        return targetEnd;
    }

    /**
     * 【方法用途】归一化：去 \r、修复被换行拆开的 URL、合并中文词中间的软换行。
     *
     * 两处绝不合并：
     * 1. 跨空行（空行是段落分隔，合并会把图片链接与其后的标题粘连）
     * 2. 下一行像列表项开头（如 "2." / "10)"）
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String src = text.replace("\r", "");
        StringBuilder out = new StringBuilder(src.length());
        boolean inUrl = false;

        for (int i = 0; i < src.length(); i++) {
            if (!inUrl && looksLikeUrlStart(src, i)) {
                inUrl = true;
            }
            char c = src.charAt(i);

            // URL 处理：URL 中间的换行可能需要合并（修复断行）
            if (inUrl) {
                if (Character.isWhitespace(c)) {
                    int j = i;
                    int newlineCount = 0;
                    while (j < src.length() && Character.isWhitespace(src.charAt(j))) {
                        if (src.charAt(j) == '\n') {
                            newlineCount++;
                        }
                        j++;
                    }
                    boolean sawNewline = newlineCount > 0;
                    boolean blankLine = newlineCount >= 2;
                    char prev = i > 0 ? src.charAt(i - 1) : 0;
                    char next = j < src.length() ? src.charAt(j) : 0;

                    if (sawNewline && !blankLine && next != 0 && shouldJoinBrokenUrl(prev, next, src, j)) {
                        i = j - 1;
                        continue;
                    }
                    out.append(src, i, j);
                    inUrl = false;
                    i = j - 1;
                    continue;
                }
                out.append(c);
                if (!isUrlChar(c) && !isCommonUrlPunct(c)) {
                    inUrl = false;
                }
                continue;
            }

            // CJK 软换行合并：商\n保通 → 商保通
            if (c == '\n') {
                char prev = i > 0 ? src.charAt(i - 1) : 0;
                char next = i + 1 < src.length() ? src.charAt(i + 1) : 0;
                if (isCjkWordChar(prev) && isCjkWordChar(next)) {
                    continue;
                }
                out.append('\n');
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * 【方法用途】判断 URL 中间的换行是否应该合并。
     */
    private static boolean shouldJoinBrokenUrl(char prev, char next, String s, int nextIndex) {
        if (isListItemStart(s, nextIndex)) {
            return false;
        }
        if (prev == '.' && Character.isLetter(next)) {
            return true;
        }
        if (prev == '/' || prev == '?' || prev == '&' || prev == '='
                || prev == '#' || prev == '%' || prev == '-' || prev == '_' || prev == ':') {
            return true;
        }
        return next == '/' || next == '?' || next == '&' || next == '=' || next == '#';
    }

    /**
     * 【方法用途】判断某位置是否像列表项开头（如 "2." / "10)"）。
     */
    private static boolean isListItemStart(String s, int i) {
        int p = i;
        while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == '\t')) {
            p++;
        }
        int start = p;
        while (p < s.length() && Character.isDigit(s.charAt(p))) {
            p++;
        }
        if (p == start) {
            return false;
        }
        return p < s.length() && (s.charAt(p) == '.' || s.charAt(p) == '）' || s.charAt(p) == ')');
    }

    private static boolean looksLikeUrlStart(String s, int i) {
        return s.startsWith("http://", i) || s.startsWith("https://", i);
    }

    private static boolean isUrlChar(char c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
            return true;
        }
        return c == '-' || c == '.' || c == '_' || c == '~'
                || c == ':' || c == '/' || c == '?' || c == '#'
                || c == '[' || c == ']' || c == '@'
                || c == '!' || c == '$' || c == '&' || c == '\''
                || c == '(' || c == ')' || c == '*' || c == '+'
                || c == ',' || c == ';' || c == '=' || c == '%';
    }

    private static boolean isCommonUrlPunct(char c) {
        return c == '.' || c == '/' || c == '?' || c == '&' || c == '=' || c == '-' || c == '_' || c == '%';
    }

    private static boolean isCjkWordChar(char c) {
        if (c == 0 || Character.isWhitespace(c)) {
            return false;
        }
        return isCjkOrFullWidthLetterOrDigit(c) && !isCjkPunctuation(c);
    }

    private static boolean isCjkOrFullWidthLetterOrDigit(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private static boolean isCjkPunctuation(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || c == '。' || c == '，' || c == '、' || c == '；' || c == '：'
                || c == '！' || c == '？' || c == '（' || c == '）' || c == '【' || c == '】'
                || c == '《' || c == '》' || c == '“' || c == '”' || c == '‘' || c == '’';
    }
}