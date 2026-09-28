package com.bitselect.agent.rag.core.rewrite;

import com.bitselect.agent.rag.dao.entity.QueryTermMappingDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 【文件用途】术语映射缓存管理器：把数据库里的映射规则缓存到 Redis。
 *
 * 【为什么存在】
 * - 每次提问都要做归一化，每次都读数据库压力大
 * - 缓存 7 天，增删改时主动清缓存
 *
 * 【被谁引用】QueryTermMappingService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryTermMappingCacheManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = "ragent:query-term:mappings";

    private static final long CACHE_EXPIRE_DAYS = 7;

    /**
     * 【方法用途】从 Redis 获取术语映射缓存，缓存不存在返回 null。
     */
    public List<QueryTermMappingDO> getMappingsFromCache() {
        try {
            String cacheJson = stringRedisTemplate.opsForValue().get(CACHE_KEY);
            if (cacheJson == null) {
                log.info("术语映射缓存不存在，需要从数据库加载");
                return null;
            }
            return objectMapper.readValue(cacheJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("从 Redis 读取术语映射缓存失败", e);
            return null;
        }
    }

    /**
     * 【方法用途】将术语映射保存到 Redis 缓存。
     */
    public void saveMappingsToCache(List<QueryTermMappingDO> mappings) {
        try {
            String cacheJson = objectMapper.writeValueAsString(mappings);
            stringRedisTemplate.opsForValue().set(CACHE_KEY, cacheJson, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            log.info("术语映射已保存到 Redis 缓存，共 {} 条规则", mappings.size());
        } catch (Exception e) {
            log.error("保存术语映射到 Redis 缓存失败", e);
        }
    }

    /**
     * 【方法用途】清除术语映射缓存：在映射规则发生增删改时调用。
     */
    public void clearCache() {
        Boolean deleted = stringRedisTemplate.delete(CACHE_KEY);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("术语映射缓存已清除");
        } else {
            log.info("术语映射缓存不存在，无需清除");
        }
    }
}