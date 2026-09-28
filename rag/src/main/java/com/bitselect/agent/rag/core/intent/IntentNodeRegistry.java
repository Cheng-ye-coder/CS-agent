package com.bitselect.agent.rag.core.intent;

import java.util.List;

/**
 * 【文件用途】意图节点注册表接口：运行期获取意图树和节点信息。
 *
 * 【为什么存在】
 * - AgentSkillAdminService 需要"当前有哪些启用的 MCP 节点"来给技能选工具
 * - 意图节点运行时需要按 ID 反查
 * - 与 IntentClassifier 接口分开：分类是核心能力，注册表是辅助查询
 *
 * 【被谁引用】AgentSkillAdminServiceImpl / RetrievalEngine
 */
public interface IntentNodeRegistry {

    /**
     * 【方法用途】根据节点 ID 获取节点。
     */
    IntentNode getNodeById(String id);

    /**
     * 【方法用途】获取当前已启用、可参与路由的 MCP 叶子节点。
     */
    List<IntentNode> listMcpToolNodes();
}