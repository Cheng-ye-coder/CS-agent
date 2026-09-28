package com.bitselect.agent.core.chunk.model;

/**
 * 分块产物：不可变，不含向量
 *
 * @param index         块在文档中的序号，从 0 开始
 * @param content       文档原貌（markdown，标题按原文位置在正文内），回填 LLM 上下文与前端预览用
 * @param embeddingText 向量文本（章节路径 + 正文），不参与展示
 */
public record Chunk(
        String chunkId,
        int index,
        String content,
        String embeddingText,
        ChunkMetadata metadata
) {

    public Chunk {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId 不能为空");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index 必须 >= 0，实际 " + index);
        }
        if (content == null) {
            throw new IllegalArgumentException("content 不能为 null，chunkId=" + chunkId);
        }
        if (embeddingText == null || embeddingText.isBlank()) {
            throw new IllegalArgumentException("embeddingText 不能为空，chunkId=" + chunkId
                    + "——向量文本由 chunker 基类统一组装章节上下文");
        }
        metadata = metadata == null ? ChunkMetadata.empty() : metadata;
    }

    public Chunk withMetadata(ChunkMetadata newMetadata) {
        return new Chunk(chunkId, index, content, embeddingText, newMetadata);
    }
}