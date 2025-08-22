package com.xzb.canal.service.impl;

import com.alibaba.fastjson.JSON;
import com.xzb.canal.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 默认缓存过期时间（24小时）
     */
    private static final long DEFAULT_EXPIRE = 24 * 60 * 60;
    
    @Override
    public void updateCache(String key, Object value) {
        try {
            String jsonString = JSON.toJSONString(value);
            redisTemplate.opsForValue().set(key, jsonString, DEFAULT_EXPIRE, TimeUnit.SECONDS);
            log.info("缓存更新成功: key={}, value={}", key, jsonString);
        } catch (Exception e) {
            log.error("缓存更新失败: key={}, value={}, error={}", key, value, e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteCache(String key) {
        try {
            redisTemplate.delete(key);
            log.info("缓存删除成功: key={}", key);
        } catch (Exception e) {
            log.error("缓存删除失败: key={}, error={}", key, e.getMessage(), e);
        }
    }
    
    @Override
    public String formatKey(String template, Object value) {
        return String.format(template, value);
    }
} 