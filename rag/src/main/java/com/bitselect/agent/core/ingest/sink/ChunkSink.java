package com.bitselect.agent.core.ingest.sink;

import com.bitselect.agent.core.chunk.model.EmbeddedChunk;
import com.bitselect.agent.core.ingest.DocumentRef;
import com.bitselect.agent.core.ingest.VectorTarget;

import java.util.List;

/**
 * 【文件用途】索引落点端口：内核只认这个接口，实现住在各自模块里。
 *
 * 【为什么存在】
 * - 内核不应知道"块要写到哪些后端"（向量库、关键词索引、图库、关系库）
 * - 用接口让内核与具体存储解耦，加一个索引后端 = 加一个 ChunkSink bean
 * - 内核注入 List<ChunkSink> 扇出，实现间先后由 Spring 的 @Order 决定
 *
 * 【关键设计】
 * - 只暴露"整体替换"而非删 + 写两个方法：先删后建的顺序由实现自己保证
 *   （向量装饰器链靠它构成 upsert 语义）
 * - 所有实现包在内核落库步骤的同一个事务里
 *
 * 【被谁引用】
 * - ChunkIndexWriter（扇出调用）
 * - 各实现：关系库 Sink、向量库 Sink、ES Sink、图库 Sink
 */
public interface ChunkSink {

    /**
     * 【方法用途】用给定的块整体替换该文档已有的块，空列表表示该文档不产生任何块。
     */
    void replaceDocument(VectorTarget target, DocumentRef doc, List<EmbeddedChunk> chunks);

    /**
     * 【方法用途】清除该文档的全部块。
     */
    void deleteDocument(VectorTarget target, DocumentRef doc);
}