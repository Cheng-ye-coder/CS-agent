

package com.bitselect.agent.infra.model;

import com.bitselect.agent.infra.config.AIModelProperties;

/**
 * 模型目标配置记录
 *
 * @param id        模型唯一标识符
 * @param candidate 模型候选配置
 * @param provider  提供商配置
 * @param timeoutMs 本次调用的超时预算（毫秒）
 */
public record ModelTarget(
        String id,
        AIModelProperties.ModelCandidate candidate,
        AIModelProperties.ProviderConfig provider,
        Long timeoutMs
) {
}