package com.bitselect.agent.rag.core.intent;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.rag.enums.IntentKind;
import com.bitselect.agent.rag.enums.IntentLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 【文件用途】意图节点：意图树中的一个节点，可以是域 / 类 / 具体主题。
 *
 * 【为什么存在】
 * - 承载意图的完整语义：名称、描述、示例问题、绑定的知识库、MCP 工具、提示词模板
 * - 叶子节点参与 LLM 意图打分；非叶子节点仅作分类
 *
 * 【关键设计】
 * - isLeaf() / isKB() / isMCP() / isSystem() 便捷判断
 * - getEffectiveCollectionNames()：新字段优先、旧字段兜底，平滑升级
 * - @JsonIgnore 标记运行时计算的字段（如 getEffectiveCollectionNames），避免污染序列化
 *
 * 【被谁引用】IntentTreeFactory / DefaultIntentClassifier / IntentResolver / RetrievalEngine
 */
@Data
@Builder
public class IntentNode {

    /**
     * 唯一标识，如 group-hr / biz-oa-intro / sales-data
     */
    private String id;

    private String kbId;

    private String name;

    /**
     * 语义说明，用于向量化时的语义提示词
     */
    private String description;

    private IntentLevel level;

    private String parentId;

    @Builder.Default
    private List<String> examples = new ArrayList<>();

    @Builder.Default
    private List<IntentNode> children = new ArrayList<>();

    /**
     * 预计算好的嵌入向量（仅向量意图识别测试使用）
     */
    @Deprecated
    @Builder.Default
    private float[] embedding = null;

    @Builder.Default
    private String fullPath = "";

    @Builder.Default
    private IntentKind kind = IntentKind.KB;

    /**
     * Milvus Collection 名称（仅对 kind=KB 有意义）
     * 仅用于兼容旧缓存和旧数据
     */
    private String collectionName;

    /**
     * 一个 KB 意图可关联多个逻辑 Collection
     */
    @Builder.Default
    private List<String> collectionNames = new ArrayList<>();

    private String mcpToolId;

    private boolean requireConfirm;

    private Integer topK;

    private String promptSnippet;

    private String promptTemplate;

    /**
     * 参数提取提示词模板（MCP 模式专属）
     */
    private String paramPromptTemplate;

    /**
     * 【方法用途】是否为叶子节点。
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    public boolean isKB() {
        return kind == null || kind == IntentKind.KB;
    }

    public boolean isMCP() {
        return kind == IntentKind.MCP;
    }

    public boolean isSystem() {
        return kind == IntentKind.SYSTEM;
    }

    /**
     * 【方法用途】返回当前意图实际参与检索的 Collection。
     *
     * 新字段 collectionNames 优先，旧的单 collectionName 字段仅作平滑升级兜底。
     */
    @JsonIgnore
    public List<String> getEffectiveCollectionNames() {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (collectionNames != null) {
            collectionNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty() && StrUtil.isNotBlank(collectionName)) {
            normalized.add(collectionName.trim());
        }
        return List.copyOf(normalized);
    }
}