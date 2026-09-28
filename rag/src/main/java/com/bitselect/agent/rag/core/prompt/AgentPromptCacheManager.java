package com.bitselect.agent.rag.core.prompt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 【文件用途】智能体提示词缓存管理器：缓存"激活智能体叠加自定义提示词之后的结果"。
 *
 * 【为什么存在】
 * - 每次对话都要解析提示词，走数据库压力大
 * - 缓存命中即可直接取用
 * - 缓存结构随槽位集合变化时递增版本号，避免旧缓存缺少新增槽位
 *
 * 【被谁引用】AgentPromptResolver / AgentProfileAdminServiceImpl
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPromptCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 缓存结构随提示词槽位集合变化时递增版本，避免旧缓存缺少新增槽位。
     */
    private static final String CACHE_KEY = "ragent:agent:resolved-prompts:v2";

    private static final long CACHE_EXPIRE_HOURS = 1;

    /**
     * 【方法用途】从缓存读取槽位到提示词的映射，缓存不存在则返回 null。
     */
    public Map<String, String> getFromCache() {
        try {
            String cacheJson = stringRedisTemplate.opsForValue().get(CACHE_KEY);
            if (cacheJson == null) {
                return null;
            }
            return objectMapper.readValue(cacheJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("从 Redis 读取智能体提示词缓存失败", e);
            return null;
        }
    }

    public void saveToCache(Map<String, String> prompts) {
        try {
            String cacheJson = objectMapper.writeValueAsString(prompts);
            stringRedisTemplate.opsForValue().set(CACHE_KEY, cacheJson, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("保存智能体提示词到 Redis 缓存失败", e);
        }
    }

    /**
     * 【方法用途】任何智能体或槽位写操作后必须调用，否则改动直到过期才生效。
     */
    public void clearCache() {
        try {
            stringRedisTemplate.delete(CACHE_KEY);
            log.info("智能体提示词缓存已清除");
        } catch (Exception e) {
            log.error("清除智能体提示词缓存失败", e);
        }
    }
}