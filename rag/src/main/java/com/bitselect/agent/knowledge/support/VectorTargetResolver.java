package com.bitselect.agent.knowledge.support;

import com.bitselect.agent.core.ingest.VectorTarget;
import com.bitselect.agent.framework.exception.ClientException;
import com.bitselect.agent.knowledge.dao.entity.KnowledgeBaseDO;
import com.bitselect.agent.rag.config.RAGDefaultProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 【文件用途】向量落点派生：知识库配置（L2）+ 部署配置（L1）→ VectorTarget。
 *
 * 【为什么存在】
 * - 单独成一个组件是为了让"落点身份怎么算出来"只有一个产生地
 * - 原先每个写向量的调用点各自从知识库取模型、各自决定要不要回落系统默认，
 *   于是上传路径用知识库配置的模型、管道路径用系统默认模型，
 *   同一个分区里混进了两种语义空间的向量
 *
 * 【关键设计】
 * - 缺配置直接失败而不是回落默认值：嵌入模型是知识库级约束性配置
 * - dimension 来自部署级 rag.default.dimension
 *
 * 【被谁引用】
 * - KnowledgeDocumentService（上传时派生落点）
 * - IngestionNode（摄取时使用）
 */
@Component
@RequiredArgsConstructor
public class VectorTargetResolver {

    private final RAGDefaultProperties ragDefaultProperties;

    /**
     * 【方法用途】派生落点，缺配置直接失败而不是回落默认值。
     */
    public VectorTarget resolve(KnowledgeBaseDO kbDO) {
        if (kbDO == null) {
            throw new ClientException("知识库不存在");
        }
        if (!StringUtils.hasText(kbDO.getEmbeddingModel())) {
            throw new ClientException("知识库未配置嵌入模型：kbId=" + kbDO.getId());
        }
        Integer dimension = ragDefaultProperties.getDimension();
        if (dimension == null || dimension <= 0) {
            throw new ClientException("部署未配置向量维度 rag.default.dimension");
        }
        return new VectorTarget(kbDO.getCollectionName(), kbDO.getEmbeddingModel(), dimension);
    }
}