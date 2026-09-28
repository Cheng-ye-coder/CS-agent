

package com.bitselect.agent.framework.convention;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐问题 grounding 片段
 * <p>
 * 由检索片段按文档取最高分、截断文本后得到，随 assistant 消息落库，
 * 供推荐追问问题生成时 grounding
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroundingChunk {

    /**
     * 文档名称 供生成追问时识别证据所属文档
     */
    private String docName;

    /**
     * 片段全文 作为追问 grounding 的证据内容
     */
    private String text;
}