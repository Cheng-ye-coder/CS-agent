package com.bitselect.agent.rag.core.retrieval.channel;

import cn.hutool.core.collection.CollUtil;
import com.bitselect.agent.rag.config.SearchChannelProperties;
import com.bitselect.agent.rag.core.intent.NodeScore;
import com.bitselect.agent.rag.core.intent.NodeScoreFilters;
import com.bitselect.agent.rag.dto.SubQuestionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【文件用途】检索作用域解析器：由引擎按子问题各算一次放进 SearchContext。
 *
 * 【为什么存在】
 * - 同一子问题里向量走全局、关键词走定向这类作用域打架必须避免
 * - 只看 KB 意图最高分：达到置信阈值才收窄
 *
 * 【被谁引用】MultiChannelRetrievalEngine
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetrievalScopeResolver {

    private final SearchChannelProperties properties;
    private final KbCollectionProvider kbCollectionProvider;

    public RetrievalScope resolve(List<SubQuestionIntent> subIntents) {
        List<String> activeCollections = kbCollectionProvider.listActiveCollections();
        List<NodeScore> kbIntents = extractKbIntents(subIntents);
        double topScore = kbIntents.stream().mapToDouble(NodeScore::getScore).max().orElse(0.0);

        if (kbIntents.isEmpty()) {
            log.info("未识别出有效 KB 意图，检索走全局作用域");
            return RetrievalScope.global(topScore, activeCollections);
        }
        double threshold = properties.getScope().getConfidenceThreshold();
        if (topScore < threshold) {
            log.info("KB 意图置信度过低（{} < {}），检索走全局作用域", topScore, threshold);
            return RetrievalScope.global(topScore, activeCollections);
        }

        Set<String> bound = new LinkedHashSet<>(NodeScoreFilters.kbCollections(kbIntents));
        Set<String> targets = new LinkedHashSet<>(bound);
        targets.retainAll(activeCollections);
        if (targets.isEmpty()) {
            log.warn("KB 意图绑定的知识库均已失效（{}），检索退化为全局作用域", bound);
            return RetrievalScope.global(topScore, activeCollections);
        }
        if (targets.size() < bound.size()) {
            bound.removeAll(targets);
            log.warn("KB 意图绑定中已失效的知识库（{}）不参与检索", bound);
        }
        List<String> supplement = activeCollections.stream()
                .filter(collection -> !targets.contains(collection))
                .toList();
        log.info("KB 意图置信度充足（{}），检索收窄到 {} 个命中库，补充范围 {} 个库", topScore, targets.size(), supplement.size());
        return new RetrievalScope(true, topScore, kbIntents, List.copyOf(targets), supplement);
    }

    private List<NodeScore> extractKbIntents(List<SubQuestionIntent> subIntents) {
        if (CollUtil.isEmpty(subIntents)) {
            return List.of();
        }
        double minScore = properties.getScope().getMinIntentScore();
        List<NodeScore> allScores = subIntents.stream()
                .flatMap(si -> si.nodeScores().stream())
                .toList();
        return NodeScoreFilters.kb(allScores, minScore).stream()
                .filter(nodeScore -> !nodeScore.getNode().getEffectiveCollectionNames().isEmpty())
                .collect(Collectors.toMap(
                        nodeScore -> nodeScore.getNode().getId(),
                        nodeScore -> nodeScore,
                        (left, right) -> left.getScore() >= right.getScore() ? left : right,
                        LinkedHashMap::new))
                .values().stream()
                .toList();
    }
}