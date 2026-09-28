package com.bitselect.agent.rag.core.vector;

import com.bitselect.agent.rag.config.RAGDefaultProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 【文件用途】PgVector 向量空间管理：HNSW 索引创建与知识库数据清理。
 *
 * 【关键设计】
 * - PG 为共享表：HNSW 索引全局一个，drop 时只删对应 collection 的行
 * - HNSW 索引创建幂等（IF NOT EXISTS）
 * - 显式注入主库 DataSource，避免与 bit 数据源冲突
 *
 * 【被谁引用】KnowledgeVectorAdminService
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rag.vector.type", havingValue = "pg")
public class PgVectorStoreAdmin implements VectorStoreAdmin {

    private final JdbcTemplate jdbcTemplate;
    private final RAGDefaultProperties ragDefaultProperties;

    public PgVectorStoreAdmin(@Qualifier("dataSource") DataSource dataSource,
                              RAGDefaultProperties ragDefaultProperties) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.ragDefaultProperties = ragDefaultProperties;
    }

    @Override
    public void ensureVectorSpace(VectorSpaceSpec spec) {
        String indexName = "idx_kv_embedding_hnsw";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indexName);
        if (count != null && count > 0) {
            log.debug("HNSW索引已存在: {}", indexName);
            return;
        }
        int dimension = ragDefaultProperties.getDimension();
        log.info("创建pgvector HNSW索引，维度: {}", dimension);
        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s ON t_knowledge_vector USING hnsw (embedding vector_cosine_ops)", indexName));
    }

    @Override
    public boolean vectorSpaceExists(VectorSpaceId spaceId) {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_knowledge_vector LIMIT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void dropVectorSpace(String collectionName) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM t_knowledge_vector WHERE collection_name = ?", collectionName);
        log.info("已删除 collection={} 的残留向量行，count={}", collectionName, deleted);
    }
}