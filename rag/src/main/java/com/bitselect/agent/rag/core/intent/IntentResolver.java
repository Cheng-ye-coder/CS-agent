package com.bitselect.agent.rag.core.intent;

import cn.hutool.core.collection.CollUtil;
import com.bitselect.agent.framework.trace.RagTraceNode;
import com.bitselect.agent.rag.core.rewrite.RewriteResult;
import com.bitselect.agent.rag.dto.IntentCandidate;
import com.bitselect.agent.rag.dto.IntentGroup;
import com.bitselect.agent.rag.dto.SubQuestionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.bitselect.agent.rag.constant.RAGConstant.INTENT_MIN_SCORE;
import static com.bitselect.agent.rag.constant.RAGConstant.MAX_INTENT_COUNT;
import static com.bitselect.agent.rag.enums.IntentKind.SYSTEM;

/**
 * 【文件用途】意图解析器：把改写结果转成"每个子问题 + 意图列表"的结构。
 *
 * 【为什么存在】
 * - 多子问题并行做意图识别
 * - 意图数量超限时按"每子问题保底 1 个 + 按分数分配剩余"策略裁剪
 * - 提供 isSystemOnly 判定，让系统交互类问题走直答
 *
 * 【被谁引用】StreamChatPipeline / KnowledgeSearchFacade / RetrievalEngine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentResolver {

    @Qualifier("defaultIntentClassifier")
    private final IntentClassifier intentClassifier;
    private final Executor intentClassifyExecutor;

    /**
     * 【方法用途】对改写结果做意图解析：按子问题并行分类，超限时裁剪。
     */
    @RagTraceNode(name = "intent-resolve", type = "INTENT")
    public List<SubQuestionIntent> resolve(RewriteResult rewriteResult) {
        List<String> subQuestions = CollUtil.isNotEmpty(rewriteResult.subQuestions())
                ? rewriteResult.subQuestions()
                : List.of(rewriteResult.rewrittenQuestion());
        List<CompletableFuture<SubQuestionIntent>> tasks = subQuestions.stream()
                .map(q -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return new SubQuestionIntent(q, classifyIntents(q));
                            } catch (Exception e) {
                                log.error("子问题意图分类失败，降级为空意图，question：{}", q, e);
                                return new SubQuestionIntent(q, List.of());
                            }
                        },
                        intentClassifyExecutor
                ))
                .toList();
        List<SubQuestionIntent> subIntents = tasks.stream()
                .map(CompletableFuture::join)
                .toList();
        return capTotalIntents(subIntents);
    }

    /**
     * 【方法用途】把所有子问题的意图合并成 KB / MCP 两组。
     */
    public IntentGroup mergeIntentGroup(List<SubQuestionIntent> subIntents) {
        List<NodeScore> mcpIntents = new ArrayList<>();
        List<NodeScore> kbIntents = new ArrayList<>();
        for (SubQuestionIntent si : subIntents) {
            mcpIntents.addAll(NodeScoreFilters.mcp(si.nodeScores()));
            kbIntents.addAll(NodeScoreFilters.kb(si.nodeScores()));
        }
        return new IntentGroup(mcpIntents, kbIntents);
    }

    /**
     * 【方法用途】判断是否只命中 SYSTEM 类型意图。
     */
    public boolean isSystemOnly(List<NodeScore> nodeScores) {
        return nodeScores.size() == 1
                && nodeScores.get(0).getNode() != null
                && nodeScores.get(0).getNode().getKind() == SYSTEM;
    }

    private List<NodeScore> classifyIntents(String question) {
        List<NodeScore> scores = intentClassifier.classifyTargets(question);
        return scores.stream()
                .filter(ns -> ns.getScore() >= INTENT_MIN_SCORE)
                .limit(MAX_INTENT_COUNT)
                .toList();
    }

    /**
     * 限制总意图数量不超过 MAX_INTENT_COUNT。
     *
     * 策略：
     * 1. 如果总数未超限，直接返回
     * 2. 如果超限，每个子问题至少保留 1 个最高分意图
     * 3. 剩余配额按分数从高到低分配给其他意图
     */
    private List<SubQuestionIntent> capTotalIntents(List<SubQuestionIntent> subIntents) {
        int totalIntents = subIntents.stream()
                .mapToInt(si -> si.nodeScores().size())
                .sum();

        if (totalIntents <= MAX_INTENT_COUNT) {
            return subIntents;
        }

        List<IntentCandidate> allCandidates = collectAllCandidates(subIntents);
        List<IntentCandidate> guaranteedIntents = selectTopIntentPerSubQuestion(allCandidates, subIntents.size());
        int remaining = MAX_INTENT_COUNT - guaranteedIntents.size();
        List<IntentCandidate> additionalIntents = selectAdditionalIntents(allCandidates, guaranteedIntents, remaining);
        return rebuildSubIntents(subIntents, guaranteedIntents, additionalIntents);
    }

    private List<IntentCandidate> collectAllCandidates(List<SubQuestionIntent> subIntents) {
        List<IntentCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < subIntents.size(); i++) {
            List<NodeScore> nodeScores = subIntents.get(i).nodeScores();
            if (CollUtil.isEmpty(nodeScores)) {
                continue;
            }
            for (NodeScore ns : nodeScores) {
                candidates.add(new IntentCandidate(i, ns));
            }
        }
        candidates.sort((a, b) -> Double.compare(b.nodeScore().getScore(), a.nodeScore().getScore()));
        return candidates;
    }

    private List<IntentCandidate> selectTopIntentPerSubQuestion(List<IntentCandidate> allCandidates, int subQuestionCount) {
        List<IntentCandidate> topIntents = new ArrayList<>();
        boolean[] selected = new boolean[subQuestionCount];

        for (IntentCandidate candidate : allCandidates) {
            int index = candidate.subQuestionIndex();
            if (!selected[index]) {
                topIntents.add(candidate);
                selected[index] = true;
            }
            if (topIntents.size() == subQuestionCount) {
                break;
            }
        }
        return topIntents;
    }

    private List<IntentCandidate> selectAdditionalIntents(List<IntentCandidate> allCandidates,
                                                          List<IntentCandidate> guaranteedIntents,
                                                          int remaining) {
        if (remaining <= 0) {
            return List.of();
        }

        List<IntentCandidate> additional = new ArrayList<>();
        for (IntentCandidate candidate : allCandidates) {
            if (guaranteedIntents.contains(candidate)) {
                continue;
            }
            additional.add(candidate);
            if (additional.size() >= remaining) {
                break;
            }
        }
        return additional;
    }

    private List<SubQuestionIntent> rebuildSubIntents(List<SubQuestionIntent> originalSubIntents,
                                                      List<IntentCandidate> guaranteedIntents,
                                                      List<IntentCandidate> additionalIntents) {
        List<IntentCandidate> allSelected = new ArrayList<>(guaranteedIntents);
        allSelected.addAll(additionalIntents);

        Map<Integer, List<NodeScore>> groupedByIndex = new HashMap<>();
        for (IntentCandidate candidate : allSelected) {
            groupedByIndex.computeIfAbsent(candidate.subQuestionIndex(), k -> new ArrayList<>())
                    .add(candidate.nodeScore());
        }

        List<SubQuestionIntent> result = new ArrayList<>();
        for (int i = 0; i < originalSubIntents.size(); i++) {
            SubQuestionIntent original = originalSubIntents.get(i);
            List<NodeScore> retained = groupedByIndex.getOrDefault(i, List.of());
            result.add(new SubQuestionIntent(original.subQuestion(), retained));
        }
        return result;
    }
}