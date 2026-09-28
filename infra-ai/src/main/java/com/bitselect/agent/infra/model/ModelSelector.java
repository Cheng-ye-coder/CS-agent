

package com.bitselect.agent.infra.model;

import cn.hutool.core.util.StrUtil;
import com.bitselect.agent.infra.config.AIModelProperties;
import com.bitselect.agent.infra.enums.ModelProvider;
import com.bitselect.agent.infra.enums.Tier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 模型选择器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelSelector {

    private final AIModelProperties properties;
    private final ModelHealthStore healthStore;

    public List<ModelTarget> selectChatCandidates(boolean thinking) {
        return selectChatCandidates(thinking, null, null);
    }

    public List<ModelTarget> selectChatCandidates(boolean thinking, Tier override) {
        return selectChatCandidates(thinking, override, null);
    }

    public List<ModelTarget> selectChatCandidates(boolean thinking, Tier override, String preferredModelId) {
        AIModelProperties.ModelGroup group = properties.getChat();
        if (group == null) {
            return List.of();
        }
        String tierName = resolveTierName(group, thinking, override);
        return buildTierTargets(group, tierName, preferredModelId, thinking);
    }

    public List<ModelTarget> selectEmbeddingCandidates() {
        return selectCandidates(properties.getEmbedding());
    }

    public List<ModelTarget> selectRerankCandidates() {
        return selectCandidates(properties.getRerank());
    }

    public List<ModelTarget> selectVlmCandidates() {
        return selectCandidates(properties.getVlm());
    }

    // ==================== chat：档位机制 ====================

    private String resolveTierName(AIModelProperties.ModelGroup group, boolean thinking, Tier override) {
        if (thinking && StrUtil.isNotBlank(group.getDeepThinkingTier())) {
            return group.getDeepThinkingTier();
        }
        if (override != null) {
            return override.getKey();
        }
        return group.getDefaultTier();
    }

    private List<ModelTarget> buildTierTargets(AIModelProperties.ModelGroup group, String tierName,
                                                  String preferredModelId, boolean requireThinking) {
        Map<String, AIModelProperties.ModelCandidate> registry = buildRegistry(group.getCandidates());

        List<String> orderedIds = new ArrayList<>();
        if (StrUtil.isNotBlank(preferredModelId)) {
            AIModelProperties.ModelCandidate preferred = registry.get(preferredModelId);
            if (preferred == null) {
                log.warn("Chat preferred 模型未在注册表登记，忽略并回退档位候选: preferredModelId={}", preferredModelId);
            } else if (requireThinking && !supportsThinking(preferred)) {
                log.warn("Chat preferred 模型不支持思考，思考请求下忽略: preferredModelId={}", preferredModelId);
            } else {
                orderedIds.add(preferredModelId);
            }
        }

        AIModelProperties.TierConfig tier = group.getTiers() == null ? null : group.getTiers().get(tierName);
        Long timeoutMs = tier == null ? null : tier.getTimeoutMs();
        if (tier == null) {
            log.warn("Chat 档位配置缺失: tier={}", tierName);
        } else {
            for (String id : tier.getCandidates()) {
                if (!orderedIds.contains(id)) {
                    orderedIds.add(id);
                }
            }
        }

        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();
        List<ModelTarget> targets = new ArrayList<>();
        for (String id : orderedIds) {
            AIModelProperties.ModelCandidate candidate = registry.get(id);
            if (candidate == null) {
                log.warn("Chat 档位候选 id 未在注册表登记: id={}, tier={}", id, tierName);
                continue;
            }
            if (Boolean.FALSE.equals(candidate.getEnabled())) {
                continue;
            }
            if (requireThinking && !supportsThinking(candidate)) {
                continue;
            }
            ModelTarget target = buildModelTarget(candidate, providers, timeoutMs);
            if (target != null) {
                targets.add(target);
            }
        }
        return targets;
    }

    private boolean supportsThinking(AIModelProperties.ModelCandidate candidate) {
        return Boolean.TRUE.equals(candidate.getSupportsThinking());
    }

    private Map<String, AIModelProperties.ModelCandidate> buildRegistry(List<AIModelProperties.ModelCandidate> candidates) {
        Map<String, AIModelProperties.ModelCandidate> registry = new LinkedHashMap<>();
        if (candidates == null) {
            return registry;
        }
        for (AIModelProperties.ModelCandidate candidate : candidates) {
            if (candidate != null) {
                registry.put(resolveId(candidate), candidate);
            }
        }
        return registry;
    }

    // ==================== embedding/rerank/vlm：defaultModel + priority ====================

    private List<ModelTarget> selectCandidates(AIModelProperties.ModelGroup group) {
        if (group == null || group.getCandidates() == null) {
            return List.of();
        }
        List<AIModelProperties.ModelCandidate> orderedCandidates =
                filterAndSortCandidates(group.getCandidates(), group.getDefaultModel());
        return buildAvailableTargets(orderedCandidates);
    }

    private List<AIModelProperties.ModelCandidate> filterAndSortCandidates(List<AIModelProperties.ModelCandidate> candidates,
                                                                           String firstChoiceModelId) {
        return candidates.stream()
                .filter(c -> c != null && !Boolean.FALSE.equals(c.getEnabled()))
                .sorted(Comparator
                        .comparing((AIModelProperties.ModelCandidate c) ->
                                !Objects.equals(resolveId(c), firstChoiceModelId))
                        .thenComparing(AIModelProperties.ModelCandidate::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AIModelProperties.ModelCandidate::getId,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private List<ModelTarget> buildAvailableTargets(List<AIModelProperties.ModelCandidate> candidates) {
        Map<String, AIModelProperties.ProviderConfig> providers = properties.getProviders();
        return candidates.stream()
                .map(candidate -> buildModelTarget(candidate, providers, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== 通用 ====================

    private ModelTarget buildModelTarget(AIModelProperties.ModelCandidate candidate,
                                         Map<String, AIModelProperties.ProviderConfig> providers,
                                         Long timeoutMs) {
        String modelId = resolveId(candidate);

        if (healthStore.isUnavailable(modelId)) {
            return null;
        }

        AIModelProperties.ProviderConfig provider = providers.get(candidate.getProvider());
        if (provider == null && !ModelProvider.NOOP.matches(candidate.getProvider())) {
            log.warn("Provider配置缺失: provider={}, modelId={}", candidate.getProvider(), modelId);
            return null;
        }

        return new ModelTarget(modelId, candidate, provider, timeoutMs);
    }

    private String resolveId(AIModelProperties.ModelCandidate candidate) {
        if (StrUtil.isNotBlank(candidate.getId())) {
            return candidate.getId();
        }
        return String.format("%s::%s",
                Objects.toString(candidate.getProvider(), "unknown"),
                Objects.toString(candidate.getModel(), "unknown"));
    }
}