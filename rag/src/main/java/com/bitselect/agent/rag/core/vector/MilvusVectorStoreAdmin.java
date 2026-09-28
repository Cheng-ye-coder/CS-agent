package com.bitselect.agent.rag.core.vector;

import com.bitselect.agent.rag.config.RAGDefaultProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.response.DeleteResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】Milvus 向量空间管理：collection 创建与知识库数据清理。
 *
 * 【关键设计】
 * - 共享 collection 模型：全 Milvus 共用一个物理 collection
 * - 各知识库以 collection_name 标量字段区分
 * - 为 collection_name 建 INVERTED 倒排索引，加速标量过滤
 *
 * 【被谁引用】KnowledgeVectorAdminService
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.vector.type", havingValue = "milvus", matchIfMissing = true)
public class MilvusVectorStoreAdmin implements VectorStoreAdmin {

    private final MilvusClientV2 milvusClient;
    private final RAGDefaultProperties ragDefaultProperties;

    @Override
    public void ensureVectorSpace(VectorSpaceSpec spec) {
        String sharedCollection = ragDefaultProperties.getCollectionName();
        boolean exists = Boolean.TRUE.equals(milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(sharedCollection).build()));
        if (exists) {
            return;
        }

        List<CreateCollectionReq.FieldSchema> fieldSchemaList = new ArrayList<>();
        fieldSchemaList.add(CreateCollectionReq.FieldSchema.builder()
                .name("id").dataType(DataType.VarChar)
                .maxLength(20).isPrimaryKey(true).autoID(false).build());
        fieldSchemaList.add(CreateCollectionReq.FieldSchema.builder()
                .name("collection_name").dataType(DataType.VarChar).maxLength(64).build());
        fieldSchemaList.add(CreateCollectionReq.FieldSchema.builder()
                .name("content").dataType(DataType.VarChar).maxLength(65535).build());
        fieldSchemaList.add(CreateCollectionReq.FieldSchema.builder()
                .name("metadata").dataType(DataType.JSON).build());
        fieldSchemaList.add(CreateCollectionReq.FieldSchema.builder()
                .name("embedding").dataType(DataType.FloatVector)
                .dimension(ragDefaultProperties.getDimension()).build());

        CreateCollectionReq.CollectionSchema collectionSchema = CreateCollectionReq.CollectionSchema
                .builder().fieldSchemaList(fieldSchemaList).build();

        IndexParam hnswIndex = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .indexName("embedding")
                .extraParams(Map.of("M", "48", "efConstruction", "200", "mmap.enabled", "false"))
                .build();

        // 共享 collection 下每次检索都是「collection_name 过滤 + ANN」，为标量字段建倒排索引
        IndexParam collectionNameIndex = IndexParam.builder()
                .fieldName("collection_name")
                .indexType(IndexParam.IndexType.INVERTED)
                .indexName("collection_name")
                .build();

        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(sharedCollection)
                .collectionSchema(collectionSchema)
                .primaryFieldName("id")
                .vectorFieldName("embedding")
                .metricType(ragDefaultProperties.getMetricType())
                .consistencyLevel(ConsistencyLevel.BOUNDED)
                .indexParams(List.of(hnswIndex, collectionNameIndex))
                .description("RAG 共享向量存储")
                .build();

        milvusClient.createCollection(createReq);
        log.info("已创建 Milvus 共享 collection: {}", sharedCollection);
    }

    @Override
    public boolean vectorSpaceExists(VectorSpaceId spaceId) {
        return Boolean.TRUE.equals(milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(ragDefaultProperties.getCollectionName()).build()));
    }

    @Override
    public void dropVectorSpace(String collectionName) {
        // 共享 collection 模型：按 collection_name 标量字段删除该知识库的行，而非 drop 整个 collection
        String filter = "collection_name == \"" + collectionName + "\"";
        DeleteResp resp = milvusClient.delete(DeleteReq.builder()
                .collectionName(ragDefaultProperties.getCollectionName())
                .filter(filter).build());
        log.info("已删除 collection_name={} 的向量行，deleteCnt={}", collectionName, resp.getDeleteCnt());
    }
}