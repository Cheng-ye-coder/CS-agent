package com.bitselect.agent.rag.dto;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.framework.convention.RetrievedChunk;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】检索上下文：MCP 与 KB 检索结果的统一承载。
 *
 * 【为什么存在】
 * - Agent 需要同时处理两类检索：工具调用（MCP）和知识库（KB）
 * - 两路结果需要统一结构传给下游（Prompt 构造、回答生成）
 * - 提供 hasMcp / hasKb / isEmpty 便捷方法，让下游快速判断
 *
 * 【关键设计】
 * - 用 @Data + @Builder 而非 record：字段较多，Builder 构造更清晰
 * - eligibleIntentIds 默认空集，避免调用方传 null
 *
 * 【被谁引用】
 * - rag 检索编排
 * - Prompt 构造与回答生成
 */
@Data
@Builder
public class RetrievalContext {

    /**
     * MCP 召回的上下文
     */
    private String mcpContext;

    /**
     * KB 召回的上下文
     */
    private String kbContext;

    /**
     * 意图 ID → 分片列表
     */
    private Map<String, List<RetrievedChunk>> intentChunks;

    /**
     * 允许参与模板选择和规则注入的意图 ID
     */
    @Builder.Default
    private Set<String> eligibleIntentIds = Set.of();

    /**
     * 【方法用途】是否存在 MCP 上下文。
     */
    public boolean hasMcp() {
        return StrUtil.isNotBlank(mcpContext);
    }

    /**
     * 【方法用途】是否存在 KB 上下文。
     */
    public boolean hasKb() {
        return StrUtil.isNotBlank(kbContext);
    }

    /**
     * 【方法用途】是否无任何上下文。
     */
    public boolean isEmpty() {
        return !hasMcp() && !hasKb();
    }
}