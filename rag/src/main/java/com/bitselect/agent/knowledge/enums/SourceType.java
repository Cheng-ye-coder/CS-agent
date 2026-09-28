

// 它定义文档从哪里来——用户上传知识库的文档，是本地文件还是远程链接。

// 来源类型	含义	典型场景
// FILE	本地文件上传	用户上传 PDF / Word / Markdown 到服务器
// URL	远程 URL 获取	给定飞书文档链接、网页 URL，系统定时抓取

package com.bitselect.agent.knowledge.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档来源类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum SourceType {

    FILE("file"),
    URL("url");

    private final String value;

    public static SourceType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if ("file".equals(normalized) || "localfile".equals(normalized) || "local_file".equals(normalized)) {
            return FILE;
        }
        if ("url".equals(normalized)) {
            return URL;
        }
        return null;
    }

    public static SourceType normalize(String value) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("来源类型不能为空");
        }
        SourceType result = fromValue(value);
        if (result == null) {
            throw new IllegalArgumentException("不支持的来源类型: " + value);
        }
        return result;
    }
}