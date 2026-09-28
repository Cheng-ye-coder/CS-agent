package com.bitselect.agent.rag.core.intent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 【文件用途】意图树缓存管理器：意图树在 Redis 中的读写。
 *
 * 【为什么存在】
 * - 意图树全量加载一次耗时（数据库 + 树构建）
 * - 用 Redis 缓存减少数据库压力；7 天过期 + 显式失效
 * - 意图节点增删改时调 clearIntentTreeCache() 主动失效
 *
 * 【被谁引用】DefaultIntentClassifier / IntentNodeAdminService（管理端）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentTreeCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String INTENT_TREE_CACHE_KEY = "ragent:intent:tree";

    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 【方法用途】从 Redis 获取意图树缓存，缓存不存在返回 null。
     */
    public List<IntentNode> getIntentTreeFromCache() {
        try {
            String cacheJson = stringRedisTemplate.opsForValue().get(INTENT_TREE_CACHE_KEY);
            if (cacheJson == null) {
                log.info("意图树缓存不存在，需要从数据库加载");
                return null;
            }
            return objectMapper.readValue(cacheJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("从Redis读取意图树缓存失败", e);
            return null;
        }
    }

    /**
     * 【方法用途】将意图树保存到 Redis 缓存。
     */
    public void saveIntentTreeToCache(List<IntentNode> roots) {
        try {
            String cacheJson = objectMapper.writeValueAsString(roots);
            stringRedisTemplate.opsForValue().set(
                    INTENT_TREE_CACHE_KEY, cacheJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("意图树已保存到Redis缓存，根节点数: {}", roots.size());
        } catch (Exception e) {
            log.error("保存意图树到Redis缓存失败", e);
        }
    }

    /**
     * 【方法用途】清除意图树缓存：在意图节点增删改时调用。
     */
    public void clearIntentTreeCache() {
        Boolean deleted = stringRedisTemplate.delete(INTENT_TREE_CACHE_KEY);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("意图树缓存已清除，Key: {}", INTENT_TREE_CACHE_KEY);
        } else {
            log.info("意图树缓存不存在，无需清除");
        }
    }

    /**
     * 【方法用途】检查缓存是否存在。
     */
    public boolean isCacheExists() {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(INTENT_TREE_CACHE_KEY));
        } catch (Exception e) {
            log.error("检查意图树缓存是否存在失败", e);
            return false;
        }
    }
}