package com.xzb.canal.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 缓存服务接口
 */
public interface CacheService {
    
    /**
     * 更新缓存
     *
     * @param key 缓存key
     * @param value 缓存值
     */
    void updateCache(String key, Object value);
    
    /**
     * 删除缓存
     *
     * @param key 缓存key
     */
    void deleteCache(String key);
    
    /**
     * 格式化缓存key
     *
     * @param template key模板，例如 "user:%d"
     * @param value 替换值，通常是主键
     * @return 格式化后的key
     */
    String formatKey(String template, Object value);
} 