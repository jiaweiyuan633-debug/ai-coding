package com.aicoding.cache;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 多级缓存：L1 Caffeine（本地，短 TTL）→ L2 Redis（分布式，长 TTL）→ Loader（数据库）
 * 适用于热点读（模板、广场、应用详情），写入侧短 TTL 最终一致
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelCache {

    private final StringRedisTemplate redisTemplate;

    private final Cache<String, String> l1 = Caffeine.newBuilder()
            .maximumSize(512)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    /**
     * 读取缓存（未命中时执行 loader 并回填两级）
     */
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        // L1
        String hit = l1.getIfPresent(key);
        if (hit != null) {
            return JSONUtil.toBean(hit, type);
        }
        // L2
        try {
            hit = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[Cache] Redis 读取失败，降级 DB: {}", e.getMessage());
        }
        if (hit != null) {
            l1.put(key, hit);
            return JSONUtil.toBean(hit, type);
        }
        // Loader
        T value = loader.get();
        if (value != null) {
            String json = JSONUtil.toJsonStr(value);
            l1.put(key, json);
            try {
                redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("[Cache] Redis 写入失败: {}", e.getMessage());
            }
        }
        return value;
    }

    public void evict(String key) {
        l1.invalidate(key);
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
}
