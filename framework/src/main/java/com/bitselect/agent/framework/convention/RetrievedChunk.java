

package com.bitselect.agent.framework.convention;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;

/**
 * RAG 检索命中结果
 * <p>
 * 表示一次向量检索或相关性搜索命中的单条记录
 * 包含原始文档片段 主键以及相关性得分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RetrievedChunk {

    /**
     * 相关性降序 缺分与非有限值沉底
     */
    public static final Comparator<RetrievedChunk> BY_SCORE_DESC = (a, b) -> Float.compare(sortScore(b), sortScore(a));

    private static float sortScore(RetrievedChunk chunk) {
        Float score = chunk.getScore();
        return score == null || !Float.isFinite(score) ? Float.NEGATIVE_INFINITY : score;
    }

    /**
     * 命中记录的唯一标识
     */
    private String id;

    /**
     * 命中的文本内容
     */
    private String text;

    /**
     * 命中得分
     */
    private Float score;

    /**
     * 精排相关度 0~1
     */
    private Float rerankScore;

    /**
     * 所属知识库 collection
     */
    private String collectionName;

    /**
     * 所属文档 ID
     */
    private String docId;

    /**
     * 分块在所属文档中的序号 从 0 开始
     */
    private Integer chunkIndex;

    /**
     * 所属文档名称
     */
    private String docName;
}