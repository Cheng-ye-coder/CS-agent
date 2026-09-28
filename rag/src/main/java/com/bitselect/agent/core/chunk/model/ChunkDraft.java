

package com.bitselect.agent.core.chunk.model;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 块草稿：切分与合并阶段的中间形态，尚未分配 ID、尚未组装向量文本
 */
public record ChunkDraft(String content, String embeddingBody, ChunkMetadata metadata,
                         boolean piece, boolean heading) {

    public ChunkDraft {
        content = content == null ? "" : content;
        metadata = metadata == null ? ChunkMetadata.empty() : metadata;
    }

    public static ChunkDraft of(String content, ChunkMetadata metadata) {
        return new ChunkDraft(content, null, metadata, false, false);
    }

    public static ChunkDraft of(String content, String embeddingBody, ChunkMetadata metadata) {
        return new ChunkDraft(content, embeddingBody, metadata, false, false);
    }

    public static ChunkDraft ofHeading(String content, String embeddingBody, ChunkMetadata metadata) {
        return new ChunkDraft(content, embeddingBody, metadata, false, true);
    }

    public static List<ChunkDraft> pieces(List<ChunkDraft> drafts) {
        if (drafts.size() <= 1) {
            return drafts;
        }
        List<ChunkDraft> marked = new ArrayList<>(drafts.size());
        for (ChunkDraft draft : drafts) {
            marked.add(new ChunkDraft(draft.content(), draft.embeddingBody(), draft.metadata(),
                    true, draft.heading()));
        }
        return marked;
    }

    public String effectiveBody() {
        return StringUtils.hasText(embeddingBody) ? embeddingBody : content;
    }

    public boolean hasExplicitBody() {
        return StringUtils.hasText(embeddingBody);
    }
}