package com.bitselect.agent.core.ingest;

/**
 * 【文件用途】文档身份：一次摄取任务的三要素（文档 ID、知识库 ID、文件名）。
 *
 * 【为什么存在】
 * - 内核执行摄取时需要知道"这批字节属于哪个文档、哪个知识库"，否则无法正确落库
 * - docId 还会传给解析器给图片资产命名 assets/{docId}/...，漏传则资产落进随机目录、与文档失联
 * - 字节从上传、URL 还是飞书来是内核之前的事，内核不认识取数方式
 *
 * 【关键设计】
 * - filename 可空：删除路径不需要文件名
 * - 构造期强制 docId 和 kbId 非空，fail-fast
 * - 提供 of(docId, kbId) 便捷构造用于删除路径
 *
 * 【被谁引用】
 * - IngestionKernel.run()
 * - ChunkSink / ChunkIndexWriter（落库归属）
 */
public record DocumentRef(String docId, String kbId, String filename) {

    public DocumentRef {
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("docId 不能为空");
        }
        if (kbId == null || kbId.isBlank()) {
            throw new IllegalArgumentException("kbId 不能为空，docId=" + docId);
        }
    }

    /**
     * 【工厂方法】删除路径用：不需要文件名。
     */
    public static DocumentRef of(String docId, String kbId) {
        return new DocumentRef(docId, kbId, null);
    }
}