package com.bitselect.agent.rag.core.vector;

/**
 * 【文件用途】向量空间元数据 / 索引管理（与检索解耦）。
 *
 * 【为什么存在】
 * - 知识库创建时需要确保向量空间存在（Milvus collection / PG HNSW 索引）
 * - 知识库删除时需要清理对应的向量行
 * - 与检索接口分开：管理操作是低频的，检索是高频的
 *
 * 【被谁引用】KnowledgeBaseService、KnowledgeVectorAdminService
 */
public interface VectorStoreAdmin {

    /**
     * 【方法用途】幂等：确保向量空间存在（不存在则创建）。
     */
    void ensureVectorSpace(VectorSpaceSpec spec);

    /**
     * 【方法用途】只判断存在性（不创建）。
     */
    boolean vectorSpaceExists(VectorSpaceId spaceId);

    /**
     * 【方法用途】幂等：销毁向量空间。
     *
     * Milvus：按 collection_name 标量字段删除该知识库的行；
     * PG：删除共享表中属于该 collection 的残留向量行（不动共享 HNSW 索引）。
     */
    void dropVectorSpace(String collectionName);
}