package com.bitselect.agent.rag.core.vector;

import com.bitselect.agent.core.chunk.model.EmbeddedChunk;

import java.util.List;

/**
 * 【文件用途】向量存储服务接口：向量写入 / 更新 / 删除的统一抽象。
 *
 * 【为什么存在】
 * - Milvus 与 PG 各一实现，由 rag.vector.type 二选一
 * - 上层 ChunkSink 只依赖这个接口，不感知具体后端
 * - 被装饰器链包裹：关键词同步 → 图谱同步 → 真实实现
 *
 * 【被谁引用】
 * - VectorChunkSink（内核落库）
 * - KeywordSyncingVectorStoreService / GraphSyncingVectorStoreService（装饰器）
 */
public interface VectorStoreService {

    /**
     * 【方法用途】批量建立文档的向量索引。
     */
    void indexDocumentChunks(String collectionName, String docId, List<EmbeddedChunk> chunks);

    /**
     * 【方法用途】更新单个 chunk 的向量索引。
     */
    void updateChunk(String collectionName, String docId, EmbeddedChunk chunk);

    /**
     * 【方法用途】删除文档的所有向量索引。
     */
    void deleteDocumentVectors(String collectionName, String docId);

    /**
     * 【方法用途】删除指定的单个 chunk 向量索引。
     */
    void deleteChunkById(String collectionName, String chunkId);

    /**
     * 【方法用途】批量删除指定 chunk 的向量索引。
     */
    void deleteChunksByIds(String collectionName, List<String> chunkIds);
}