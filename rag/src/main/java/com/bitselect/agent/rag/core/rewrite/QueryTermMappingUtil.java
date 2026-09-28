package com.bitselect.agent.rag.core.rewrite;

/**
 * 【文件用途】查询词映射工具：安全地把用户口语化的短语替换成规范术语。
 *
 * 【为什么存在】
 * - 用户说"平安保司"，知识库里写的是"平安保险"，需要映射
 * - 简单字符串替换会重复替换已规范化的词，需要防重
 *
 * 【被谁引用】QueryTermMappingService
 */
public class QueryTermMappingUtil {

    /**
     * 【方法用途】安全归一化替换。
     *
     * 只替换 sourceTerm；
     * 如果当前位置本身已经是 targetTerm 起始（例如文本中已经是"平安保司"），则不重复替换。
     */
    public static String applyMapping(String text, String sourceTerm, String targetTerm) {
        if (text == null || text.isEmpty() || sourceTerm == null || sourceTerm.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        int idx = 0;
        int len = text.length();
        int sourceLen = sourceTerm.length();
        int targetLen = targetTerm.length();

        while (idx < len) {
            int hit = text.indexOf(sourceTerm, idx);
            if (hit < 0) {
                sb.append(text, idx, len);
                break;
            }

            sb.append(text, idx, hit);

            boolean alreadyTarget =
                    targetTerm != null
                            && hit + targetLen <= len
                            && text.startsWith(targetTerm, hit);

            if (alreadyTarget) {
                sb.append(text, hit, hit + targetLen);
                idx = hit + targetLen;
            } else {
                sb.append(targetTerm);
                idx = hit + sourceLen;
            }
        }

        return sb.toString();
    }
}