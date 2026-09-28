package com.bitselect.agent.rag.core.intent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bitselect.agent.framework.convention.ChatMessage;
import com.bitselect.agent.framework.convention.ChatRequest;
import com.bitselect.agent.infra.chat.LLMService;
import com.bitselect.agent.infra.util.LLMResponseCleaner;
import com.bitselect.agent.infra.util.LogSafe;
import com.bitselect.agent.rag.core.prompt.PromptTemplateLoader;
import com.bitselect.agent.rag.dao.entity.IntentNodeDO;
import com.bitselect.agent.rag.dao.mapper.IntentNodeMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bitselect.agent.rag.constant.RAGConstant.INTENT_CLASSIFIER_PROMPT_PATH;

/**
 * 【文件用途】LLM 树形意图分类器（串行实现）：把所有叶子意图一次性发给 LLM 打分。
 *
 * 【为什么存在】
 * - 意图识别的核心实现
 * - 从 Redis 加载意图树，先构造 Prompt 让 LLM 打分，再解析 JSON
 * - 同时实现 IntentNodeRegistry，对外提供节点查询
 *
 * 【关键设计】
 * - 意图树缓存失效时从数据库加载并回填缓存
 * - 解析失败或 LLM 调用失败返回空意图，由下游兜底
 * - 降序排序后返回
 *
 * 【被谁引用】IntentResolver
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIntentClassifier implements IntentClassifier, IntentNodeRegistry {

    private final LLMService llmService;
    private final IntentNodeMapper intentNodeMapper;
    private final PromptTemplateLoader promptTemplateLoader;
    private final IntentTreeCacheManager intentTreeCacheManager;

    @Override
    public IntentNode getNodeById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        IntentTreeData data = loadIntentTreeData();
        return data.id2Node.get(id);
    }

    @Override
    public List<IntentNode> listMcpToolNodes() {
        return loadIntentTreeData().leafNodes.stream()
                .filter(IntentNode::isMCP)
                .filter(node -> node.getMcpToolId() != null && !node.getMcpToolId().isBlank())
                .sorted(Comparator.comparing(IntentNode::getId))
                .toList();
    }

    /**
     * 意图树数据结构（临时对象，不持久化）。
     */
    private record IntentTreeData(
            List<IntentNode> allNodes,
            List<IntentNode> leafNodes,
            Map<String, IntentNode> id2Node
    ) {
    }

    private IntentTreeData loadIntentTreeData() {
        List<IntentNode> roots = intentTreeCacheManager.getIntentTreeFromCache();

        if (CollUtil.isEmpty(roots)) {
            roots = loadIntentTreeFromDB();
            if (!roots.isEmpty()) {
                intentTreeCacheManager.saveIntentTreeToCache(roots);
            }
        }

        if (CollUtil.isEmpty(roots)) {
            return new IntentTreeData(List.of(), List.of(), Map.of());
        }

        List<IntentNode> allNodes = flatten(roots);
        List<IntentNode> leafNodes = allNodes.stream()
                .filter(IntentNode::isLeaf)
                .collect(Collectors.toList());
        Map<String, IntentNode> id2Node = allNodes.stream()
                .collect(Collectors.toMap(IntentNode::getId, n -> n));

        log.debug("意图树数据加载完成, 总节点数: {}, 叶子节点数: {}", allNodes.size(), leafNodes.size());

        return new IntentTreeData(allNodes, leafNodes, id2Node);
    }

    private List<IntentNode> flatten(List<IntentNode> roots) {
        List<IntentNode> result = new ArrayList<>();
        Deque<IntentNode> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            IntentNode n = stack.pop();
            result.add(n);
            if (n.getChildren() != null) {
                for (IntentNode child : n.getChildren()) {
                    stack.push(child);
                }
            }
        }
        return result;
    }

    @Override
    public List<NodeScore> classifyTargets(String question) {
        IntentTreeData data = loadIntentTreeData();
        if (data.leafNodes.isEmpty()) {
            log.debug("意图树没有可用叶子节点，跳过 LLM 意图识别");
            return List.of();
        }

        String systemPrompt = buildPrompt(data.leafNodes);
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system(systemPrompt),
                        ChatMessage.user(question)
                ))
                .temperature(0.1D)
                .topP(0.3D)
                .thinking(false)
                .build();

        String raw;
        try {
            raw = llmService.chat(request);
        } catch (Exception e) {
            log.warn("意图识别 LLM 调用失败，返回空意图", e);
            return List.of();
        }
        return parseScores(raw, data, question);
    }

    private List<NodeScore> parseScores(String raw, IntentTreeData data, String question) {
        try {
            String cleanedRaw = LLMResponseCleaner.stripMarkdownCodeFence(raw);
            JsonElement root = JsonParser.parseString(cleanedRaw);
            JsonArray arr;
            if (root.isJsonArray()) {
                arr = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("results")) {
                arr = root.getAsJsonObject().getAsJsonArray("results");
            } else {
                log.warn("LLM 返回了非预期的 JSON 格式, 原始响应: {}", LogSafe.preview(raw));
                return List.of();
            }

            List<NodeScore> scores = new ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                if (!obj.has("id") || !obj.has("score")) continue;

                String id = obj.get("id").getAsString();
                IntentNode node = data.id2Node.get(id);
                if (node == null) {
                    log.warn("LLM 返回了未知的意图节点 ID: {}, 已跳过", id);
                    continue;
                }

                scores.add(new NodeScore(node, obj.get("score").getAsDouble()));
            }

            scores.sort(Comparator.comparingDouble(NodeScore::getScore).reversed());

            log.info("当前问题：{}\n意图识别树如下所示：{}\n",
                    question,
                    JSONUtil.toJsonPrettyStr(
                            scores.stream().peek(each -> {
                                IntentNode node = each.getNode();
                                node.setChildren(null);
                            }).collect(Collectors.toList())
                    )
            );
            return scores;
        } catch (Exception e) {
            log.warn("意图打分解析失败, 原始响应: {}", LogSafe.preview(raw), e);
            return List.of();
        }
    }

    @Override
    public List<NodeScore> topKAboveThreshold(String question, int topN, double minScore) {
        return classifyTargets(question).stream()
                .filter(ns -> ns.getScore() >= minScore)
                .limit(topN)
                .toList();
    }

    private String buildPrompt(List<IntentNode> leafNodes) {
        StringBuilder sb = new StringBuilder();

        for (IntentNode node : leafNodes) {
            sb.append("- id=").append(node.getId()).append("\n");
            sb.append("  path=").append(node.getFullPath()).append("\n");
            sb.append("  description=").append(node.getDescription()).append("\n");

            if (node.isMCP()) {
                sb.append("  type=MCP\n");
                if (node.getMcpToolId() != null) {
                    sb.append("  toolId=").append(node.getMcpToolId()).append("\n");
                }
            } else if (node.isSystem()) {
                sb.append("  type=SYSTEM\n");
            } else {
                sb.append("  type=KB\n");
            }

            if (node.getExamples() != null && !node.getExamples().isEmpty()) {
                sb.append("  examples=");
                sb.append(String.join(" / ", node.getExamples()));
                sb.append("\n");
            }
            sb.append("\n");
        }

        return promptTemplateLoader.render(
                INTENT_CLASSIFIER_PROMPT_PATH,
                Map.of("intent_list", sb.toString())
        );
    }

    private List<IntentNode> loadIntentTreeFromDB() {
        List<IntentNodeDO> intentNodeDOList = intentNodeMapper.selectList(
                Wrappers.lambdaQuery(IntentNodeDO.class)
                        .eq(IntentNodeDO::getDeleted, 0)
                        .eq(IntentNodeDO::getEnabled, 1)
        );

        if (intentNodeDOList.isEmpty()) {
            return List.of();
        }

        Map<String, IntentNode> id2Node = new HashMap<>();
        for (IntentNodeDO each : intentNodeDOList) {
            IntentNode node = BeanUtil.toBean(each, IntentNode.class);
            node.setId(each.getIntentCode());
            node.setParentId(each.getParentCode());
            node.setMcpToolId(each.getMcpToolId());
            node.setRequireConfirm(Objects.equals(each.getRequireConfirm(), 1));
            node.setParamPromptTemplate(each.getParamPromptTemplate());
            node.setExamples(parseExamples(each.getExamples()));
            if (CollUtil.isEmpty(each.getCollectionNames())) {
                node.setCollectionNames(
                        each.getCollectionName() == null || each.getCollectionName().isBlank()
                                ? List.of()
                                : List.of(each.getCollectionName())
                );
            }
            if (node.getChildren() == null) {
                node.setChildren(new ArrayList<>());
            }
            id2Node.put(node.getId(), node);
        }

        List<IntentNode> roots = new ArrayList<>();
        for (IntentNode node : id2Node.values()) {
            String parentId = node.getParentId();
            if (parentId == null || parentId.isBlank()) {
                roots.add(node);
                continue;
            }

            IntentNode parent = id2Node.get(parentId);
            if (parent == null) {
                roots.add(node);
                continue;
            }

            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(node);
        }

        fillFullPath(roots, null);
        return roots;
    }

    private List<String> parseExamples(String examples) {
        if (examples == null || examples.isBlank()) {
            return List.of();
        }
        try {
            JsonElement root = JsonParser.parseString(examples);
            if (!root.isJsonArray()) {
                log.warn("意图节点 examples 不是 JSON 数组, 原始值: {}", LogSafe.preview(examples));
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    result.add(el.getAsString());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("意图节点 examples 解析失败, 原始值: {}", LogSafe.preview(examples), e);
            return List.of();
        }
    }

    private void fillFullPath(List<IntentNode> nodes, IntentNode parent) {
        if (nodes == null) return;

        for (IntentNode node : nodes) {
            if (parent == null) {
                node.setFullPath(node.getName());
            } else {
                node.setFullPath(parent.getFullPath() + " > " + node.getName());
            }

            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                fillFullPath(node.getChildren(), node);
            }
        }
    }
}