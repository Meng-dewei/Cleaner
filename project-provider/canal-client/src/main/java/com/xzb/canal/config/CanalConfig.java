package com.xzb.canal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Canal配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "canal")
public class CanalConfig {
    
    /**
     * RocketMQ主题名
     */
    private String topic;
    
    /**
     * 需要监听的表配置
     */
    private List<TableConfig> tables;
    
    /**
     * 表配置
     */
    @Data
    public static class TableConfig {
        /**
         * 数据库名
         */
        private String database;
        
        /**
         * 表名
         */
        private String table;
        
        /**
         * 缓存key模板, 如 "user:%d" 中的 %d 会被替换为主键值
         */
        private String cacheKey;
    }
} 