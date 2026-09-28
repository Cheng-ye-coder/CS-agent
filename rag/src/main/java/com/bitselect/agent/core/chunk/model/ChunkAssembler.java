package com.bitselect.agent.core.chunk.model;

import cn.hutool.core.util.IdUtil;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 块装配器：草稿 → 成品块的唯一通道
 */
public final class ChunkAssembler {

    private static final String CONTEXT_SEPARATOR = "\n";

    private static final String OUTLINE_SEPARATOR = " / ";

    private ChunkAssembler() {
    }

    public static List<Chunk> assembleAll(List<ChunkDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return List.of();
        }
        List<Chunk> chunks = new ArrayList<>(drafts.size());
        for (int i = 0; i < drafts.size(); i++) {
            chunks.add(assemble(i, drafts.get(i)));
        }
        return chunks;
    }

    public static Chunk assemble(int index, ChunkDraft draft) {
        return assemble(nextChunkId(), index, draft);
    }

    public static Chunk assemble(String chunkId, int index, ChunkDraft draft) {
        ChunkMetadata metadata = draft.metadata();
        return new Chunk(chunkId, index, draft.content(),
                composeEmbeddingText(metadata, draft.effectiveBody(), draft.content()), metadata);
    }

    public static Chunk restore(String chunkId, int index, String content, String embeddingText) {
        return new Chunk(chunkId, index, content,
                StringUtils.hasText(embeddingText) ? embeddingText : content, ChunkMetadata.empty());
    }

    public static String nextChunkId() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private static String composeEmbeddingText(ChunkMetadata metadata, String body, String content) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, missingOutlinePrefix(metadata, content));
        appendIfPresent(sb, body);
        return sb.toString();
    }

    private static String missingOutlinePrefix(ChunkMetadata metadata, String content) {
        List<String> path = metadata.outlinePath();
        int keep = 0;
        while (keep < path.size() && !content.contains(path.get(keep))) {
            keep++;
        }
        return keep == 0 ? null : String.join(OUTLINE_SEPARATOR, path.subList(0, keep));
    }

    private static void appendIfPresent(StringBuilder sb, String part) {
        if (!StringUtils.hasText(part)) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(CONTEXT_SEPARATOR);
        }
        sb.append(part.strip());
    }
}