package com.bitselect.agent.rag.core.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 【文件用途】向量检索请求参数：query + topK + 可选 collection 过滤 + 可选 metadata 过滤。
 *
 * 【为什么存在】
 * - 向量检索需要统一的入参结构
 * - 支持单库和跨库检索：单库传 collectionName，跨库传 collectionNames
 * - 保留 metadataFilters 作为未来扩展（当前未使用）
 *
 * 【被谁引用】VectorRetrieverService / VectorSearchChannel
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrieveRequest {

    private String query;

    @Builder.Default
    private int topK = 5;

    private String collectionName;

    private List<String> collectionNames;

    private Map<String, Object> metadataFilters;

    /**
     * 【方法用途】多 Collection 参数优先，旧的单 Collection 参数用于兼容已有调用方。
     */
    public List<String> getEffectiveCollectionNames() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (collectionNames != null) {
            collectionNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty() && collectionName != null && !collectionName.isBlank()) {
            normalized.add(collectionName.trim());
        }
        return List.copyOf(normalized);
    }
}