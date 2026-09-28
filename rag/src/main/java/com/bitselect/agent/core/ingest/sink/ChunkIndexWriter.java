package com.bitselect.agent.core.ingest.sink;

import com.bitselect.agent.core.chunk.model.EmbeddedChunk;
import com.bitselect.agent.core.ingest.DocumentRef;
import com.bitselect.agent.core.ingest.VectorTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;

/**
 * 【文件用途】索引扇出：把块整体写进全部落点，事务边界在此。
 *
 * 【为什么存在】
 * - 内核不应知道具体有几个 Sink、它们各自的写入逻辑
 * - 需要统一的事务边界：所有 Sink 要么全成功、要么全回滚
 * - 加一个索引后端 = 加一个 ChunkSink bean，本类一行不改
 *
 * 【关键设计】
 * - 注入 List<ChunkSink>，Spring 会按 @Order 排序后注入
 * - 用 TransactionOperations.executeWithoutResult 包住全部 Sink 调用
 * - 写入完成后打印汇总日志（docId / 分区 / 块数 / 落点数）
 *
 * 【被谁引用】DefaultIngestionKernel（第 ⑤ 步 index）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkIndexWriter {

    private final List<ChunkSink> sinks;
    private final TransactionOperations transactionOperations;

    /**
     * 【方法用途】整体替换该文档的块：全部落点在同一个事务里。
     */
    public void replaceDocument(VectorTarget target, DocumentRef doc, List<EmbeddedChunk> chunks) {
        transactionOperations.executeWithoutResult(status ->
                sinks.forEach(sink -> sink.replaceDocument(target, doc, chunks)));
        log.info("块索引写入完成 docId={} 分区={} 块数={} 落点数={}",
                doc.docId(), target.partition(), chunks.size(), sinks.size());
    }

    /**
     * 【方法用途】删除该文档的全部块：全部落点在同一个事务里。
     */
    public void deleteDocument(VectorTarget target, DocumentRef doc) {
        transactionOperations.executeWithoutResult(status ->
                sinks.forEach(sink -> sink.deleteDocument(target, doc)));
    }
}