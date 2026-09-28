package com.bitselect.agent.rag.core.rewrite;

import com.bitselect.agent.framework.convention.ChatMessage;

import java.util.List;

/**
 * 【文件用途】用户查询改写服务接口：将自然语言问题改写成适合 RAG 检索的查询语句。
 *
 * 【为什么存在】
 * - 用户提问口语化、包含指代、可能有多个子问题
 * - 改写后能显著提升检索命中率
 * - 接口隔离实现，便于替换改写策略
 *
 * 【被谁引用】StreamChatPipeline / KnowledgeSearchFacade
 */
public interface QueryRewriteService {

    /**
     * 【方法用途】将用户问题改写为适合向量 / 关键字检索的简洁查询。
     *
     * @param userQuestion 原始用户问题
     * @return 改写后的检索查询（如果改写失败，则回退原问题）
     */
    String rewrite(String userQuestion);

    /**
     * 【方法用途】可选：改写 + 拆分多问句。
     *
     * 默认实现仅返回改写结果并将其作为单个子问题。
     */
    default RewriteResult rewriteWithSplit(String userQuestion) {
        String rewritten = rewrite(userQuestion);
        return new RewriteResult(rewritten, List.of(rewritten));
    }

    /**
     * 【方法用途】可选：改写 + 拆分多问句，支持会话历史。
     *
     * 默认实现忽略历史，回退到基础改写逻辑。
     */
    default RewriteResult rewriteWithSplit(String userQuestion, List<ChatMessage> history) {
        return rewriteWithSplit(userQuestion);
    }
}