package com.bitselect.agent.rag.core.retrieval.channel;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bitselect.agent.knowledge.dao.entity.KnowledgeBaseDO;
import com.bitselect.agent.knowledge.dao.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 【文件用途】有效知识库 collection 提供者：全局检索的唯一"全库范围"来源。
 *
 * 【为什么存在】
 * - 全局检索（向量 / 关键词）需要知道"现在有哪些有效知识库"
 * - 两路全局检索共用此处，保证"全局"语义一致——都以知识库表为准
 * - 不用通配（如 ES 的 kb_*）：后者会命中已删除库残留、测试库、旧 schema 等无效索引
 *
 * 【被谁引用】RetrievalScopeResolver
 */
@Component
@RequiredArgsConstructor
public class KbCollectionProvider {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public List<String> listActiveCollections() {
        List<KnowledgeBaseDO> kbList = knowledgeBaseMapper.selectList(
                Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                        .select(KnowledgeBaseDO::getCollectionName)
                        .eq(KnowledgeBaseDO::getDeleted, 0)
        );
        return kbList.stream()
                .map(KnowledgeBaseDO::getCollectionName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }
}