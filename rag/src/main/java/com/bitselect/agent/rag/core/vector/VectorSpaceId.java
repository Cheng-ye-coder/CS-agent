package com.bitselect.agent.rag.core.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 【文件用途】向量空间标识：对业务层暴露的逻辑名称 + 可选命名空间。
 *
 * 【为什么存在】
 * - 屏蔽不同向量后端（Milvus / PG）的物理差异
 * - 业务代码用逻辑名，后端负责映射到具体物理空间
 *
 * 【被谁引用】VectorSpaceSpec
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorSpaceId {

    /**
     * 逻辑名称：对业务层暴露的名字，跨引擎保持一致
     */
    String logicalName;

    /**
     * 可选：命名空间 / 数据库 / 索引前缀
     */
    String namespace;
}