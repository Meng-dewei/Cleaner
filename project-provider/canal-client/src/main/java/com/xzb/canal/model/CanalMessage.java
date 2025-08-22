package com.xzb.canal.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Canal消息模型，用于解析RocketMQ中的Canal消息
 */
@Data
public class CanalMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 数据
     */
    private List<CanalData> data;
    
    /**
     * 数据库名
     */
    private String database;
    
    /**
     * 表名
     */
    private String table;
    
    /**
     * 类型：INSERT、UPDATE、DELETE等
     */
    private String type;
    
    /**
     * 执行时间
     */
    private Long es;
    
    /**
     * 同步的ID
     */
    private String id;
    
    /**
     * 是否DDL语句
     */
    private Boolean isDdl;
    
    /**
     * 事务ID
     */
    private String xid;
    
    /**
     * 事务内SQL序号
     */
    private String xoffset;
    
    /**
     * SQL语句
     */
    private String sql;
    
    /**
     * 数据变更记录
     */
    @Data
    public static class CanalData {
        /**
         * 变更前数据
         */
        private JSONObject old;
        
        /**
         * 变更后数据
         */
        private JSONObject data;
    }
} 