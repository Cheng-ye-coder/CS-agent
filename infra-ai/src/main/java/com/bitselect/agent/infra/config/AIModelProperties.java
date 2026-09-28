

package com.bitselect.agent.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型配置属性类
 * 用于从配置文件中读取 AI 相关的配置信息，包括提供商配置、模型组配置等
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIModelProperties {

    /**
     * AI 提供商配置映射
     * key: 提供商名称，value: 提供商配置信息
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    /**
     * 聊天模型组配置
     */
    private ModelGroup chat = new ModelGroup();

    /**
     * 向量嵌入模型组配置
     */
    private ModelGroup embedding = new ModelGroup();

    /**
     * 重排序模型组配置
     */
    private ModelGroup rerank = new ModelGroup();

    /**
     * 视觉大模型组配置（图生文，知识库入库期使用）
     */
    private ModelGroup vlm = new ModelGroup();

    /**
     * 模型选择策略配置
     */
    private Selection selection = new Selection();

    /**
     * 流式响应配置
     */
    private Stream stream = new Stream();

    /**
     * 模型组配置类
     */
    @Data
    public static class ModelGroup {
        /**
         * 默认使用的模型标识（embedding/rerank/vlm 使用；chat 已改用 tier）
         */
        private String defaultModel;

        /**
         * 候选模型列表
         */
        private List<ModelCandidate> candidates = new ArrayList<>();

        /**
         * 默认档位名（仅 chat 使用）
         */
        private String defaultTier;

        /**
         * 深度思考档位名（仅 chat 使用）
         */
        private String deepThinkingTier;

        /**
         * 档位配置映射（仅 chat 使用）
         */
        private Map<String, TierConfig> tiers = new HashMap<>();
    }

    /**
     * 档位配置类
     */
    @Data
    public static class TierConfig {

        /**
         * 有序候选模型 id 列表
         */
        private List<String> candidates = new ArrayList<>();

        /**
         * 该档位的调用超时预算（毫秒）
         */
        private Long timeoutMs;
    }

    /**
     * 模型候选配置类
     */
    @Data
    public static class ModelCandidate {

        private String id;
        private String provider;
        private String model;
        private String url;
        private Integer dimension;
        private Integer priority = 100;
        private Boolean enabled = true;
        private Boolean supportsThinking = false;
    }

    /**
     * 提供商配置类
     */
    @Data
    public static class ProviderConfig {

        private String url;
        private String apiKey;
        private Map<String, String> endpoints = new HashMap<>();
    }

    /**
     * 模型选择策略配置类
     */
    @Data
    public static class Selection {

        private Integer failureThreshold = 2;
        private Long openDurationMs = 30000L;
    }

    /**
     * 流式响应配置类
     */
    @Data
    public static class Stream {

        private Integer messageChunkSize = 5;
    }
}