package com.bitselect.agent.rag.core.vector.sink;

import com.bitselect.agent.core.chunk.model.EmbeddedChunk;
import com.bitselect.agent.core.ingest.DocumentRef;
import com.bitselect.agent.core.ingest.VectorTarget;
import com.bitselect.agent.core.ingest.sink.ChunkSink;
import com.bitselect.agent.rag.core.vector.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 【文件用途】向量落点：内核的 ChunkSink 实现，委托给 VectorStoreService。
 *
 * 【为什么存在】
 * - 让向量存储成为内核扇出的一环（与关系库 / ES / 图库平级）
 * - 注入的 VectorStoreService 是装饰器链（图谱 → 关键词 → PG/Milvus）
 * - 要把关键词或图谱提升为一等落点，各加一个 ChunkSink bean、删对应装饰器即可
 *
 * 【关键设计】
 * - @Order(LOWEST_PRECEDENCE)：向量写入放最后，让其他 Sink 先执行
 * - 先删后建：装饰器链的图谱同步依赖这个顺序构成 upsert 语义
 *
 * 【被谁引用】ChunkIndexWriter（内核扇出）
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class VectorChunkSink implements ChunkSink {

    private final VectorStoreService vectorStoreService;

    @Override
    public void replaceDocument(VectorTarget target, DocumentRef doc, List<EmbeddedChunk> chunks) {
        // 先删后建：装饰器链的图谱同步正是依赖这个顺序构成 upsert 语义
        vectorStoreService.deleteDocumentVectors(target.partition(), doc.docId());
        if (!chunks.isEmpty()) {
            vectorStoreService.indexDocumentChunks(target.partition(), doc.docId(), chunks);
        }
    }

    @Override
    public void deleteDocument(VectorTarget target, DocumentRef doc) {
        vectorStoreService.deleteDocumentVectors(target.partition(), doc.docId());
    }
}