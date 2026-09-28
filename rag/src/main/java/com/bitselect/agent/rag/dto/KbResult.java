package com.bitselect.agent.rag.dto;

import com.bitselect.agent.framework.convention.RetrievedChunk;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】知识库检索结果：一次 KB 检索返回的完整信息。
 *
 * 【为什么存在】
 * - 检索结果需要携带三种信息：拼好的上下文文本、意图到分片的映射、参与后续处理的意图白名单
 * - 用 record 让这三个"必须一起流转"的字段被封装
 *
 * 【关键字段】
 * - groupedContext：已按意图分组拼好的上下文文本（喂给 LLM）
 * - intentChunks：意图 ID → 该意图命中的分片列表
 * - eligibleIntentIds：允许参与模板选择和规则注入的意图 ID（过滤掉低分意图）
 *
 * 【被谁引用】
 * - rag 检索编排（构建 RetrievalContext）
 */
public record KbResult(String groupedContext,
                       Map<String, List<RetrievedChunk>> intentChunks,
                       Set<String> eligibleIntentIds) {
}