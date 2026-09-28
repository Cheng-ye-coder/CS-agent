package com.bitselect.agent.rag.core.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】向量空间规格：确保空间存在时传入的完整描述。
 *
 * 【为什么存在】
 * - VectorStoreAdmin.ensureVectorSpace 需要一个规格对象
 * - 未来可扩展（如加 collection 描述、TTL 等）
 *
 * 【被谁引用】VectorStoreAdmin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorSpaceSpec {

    /**
     * 向量空间标识
     */
    private VectorSpaceId spaceId;

    /**
     * 备注
     */
    private String remark;
}