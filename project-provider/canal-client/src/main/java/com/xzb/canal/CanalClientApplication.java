package com.xzb.canal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Canal客户端应用，用于监听数据库变化并同步缓存
 */
@SpringBootApplication
public class CanalClientApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CanalClientApplication.class, args);
    }
} 