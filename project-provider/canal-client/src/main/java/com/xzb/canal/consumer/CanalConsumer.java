package com.xzb.canal.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xzb.canal.config.CanalConfig;
import com.xzb.canal.model.CanalMessage;
import com.xzb.canal.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Canal消息消费者
 * 处理数据库变更事件，并同步更新缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${canal.topic}",
        consumerGroup = "${rocketmq.consumer.group}"
)
public class CanalConsumer implements RocketMQListener<String> {
    
    private final CanalConfig canalConfig;
    private final CacheService cacheService;
    
    /**
     * 表配置映射
     */
    private Map<String, CanalConfig.TableConfig> tableConfigMap;
    
    /**
     * 初始化表配置映射
     */
    private void initTableConfigMap() {
        if (tableConfigMap == null) {
            tableConfigMap = canalConfig.getTables().stream()
                    .collect(Collectors.toMap(
                            tc -> tc.getDatabase() + "." + tc.getTable(),
                            Function.identity()
                    ));
        }
    }
    
    @Override
    public void onMessage(String message) {
        try {
            log.info("收到Canal消息: {}", message);
            initTableConfigMap();
            
            // 解析消息 - 可能是一个包含多个消息的数组(如果配置了flatMessage=true)
            if (message.startsWith("[") && message.endsWith("]")) {
                // 处理数组格式消息 - 包含一个事务的多个表变更
                List<CanalMessage> messageList = JSON.parseArray(message, CanalMessage.class);
                log.info("接收到事务消息，包含 {} 个表的变更", messageList.size());
                
                // 分组处理所有消息
                processTransactionMessages(messageList);
            } else {
                // 单个消息处理
                CanalMessage canalMessage = JSON.parseObject(message, CanalMessage.class);
                // 检查是否为我们感兴趣的表变更
                String tableKey = canalMessage.getDatabase() + "." + canalMessage.getTable();
                CanalConfig.TableConfig tableConfig = tableConfigMap.get(tableKey);
                if (tableConfig == null) {
                    log.debug("忽略非目标表的变更: {}", tableKey);
                    return;
                }
                
                // 处理变更数据
                processDataChange(canalMessage, tableConfig);
            }
            
        } catch (Exception e) {
            log.error("处理Canal消息失败: {}", message, e);
        }
    }
    
    /**
     * 处理包含多个表的事务消息
     */
    private void processTransactionMessages(List<CanalMessage> messageList) {
        // 按事务ID分组处理
        Map<String, List<CanalMessage>> transactionGroups = new HashMap<>();
        
        for (CanalMessage message : messageList) {
            String xid = message.getXid(); // 事务ID
            if (xid != null && !xid.isEmpty()) {
                transactionGroups.computeIfAbsent(xid, k -> new ArrayList<>()).add(message);
            } else {
                // 没有事务ID的消息单独处理
                processMessageWithTableConfig(message);
            }
        }
        
        // 处理每个事务组
        for (Map.Entry<String, List<CanalMessage>> entry : transactionGroups.entrySet()) {
            String xid = entry.getKey();
            List<CanalMessage> messagesInTransaction = entry.getValue();
            
            log.info("处理事务: {}, 包含 {} 个表变更", xid, messagesInTransaction.size());
            
            // 开始处理事务中的每个消息
            for (CanalMessage message : messagesInTransaction) {
                processMessageWithTableConfig(message);
            }
        }
    }
    
    /**
     * 根据表配置处理消息
     */
    private void processMessageWithTableConfig(CanalMessage message) {
        String tableKey = message.getDatabase() + "." + message.getTable();
        CanalConfig.TableConfig tableConfig = tableConfigMap.get(tableKey);
        
        if (tableConfig != null) {
            processDataChange(message, tableConfig);
        } else {
            log.debug("忽略非目标表的变更: {}", tableKey);
        }
    }
    
    /**
     * 处理数据变更
     */
    private void processDataChange(CanalMessage canalMessage, CanalConfig.TableConfig tableConfig) {
        // 一个批次可能包含多个数据变更
        List<CanalMessage.CanalData> dataList = canalMessage.getData();
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        
        String type = canalMessage.getType();
        log.info("处理 {} 类型的数据变更，表: {}.{}, 变更数量: {}, 事务ID: {}", 
                type, canalMessage.getDatabase(), canalMessage.getTable(), 
                dataList.size(), canalMessage.getXid());
        
        // 处理每一个数据变更
        for (CanalMessage.CanalData canalData : dataList) {
            try {
                switch (type) {
                    case "INSERT":
                    case "UPDATE":
                        handleInsertOrUpdate(canalData, tableConfig);
                        break;
                    case "DELETE":
                        handleDelete(canalData, tableConfig);
                        break;
                    default:
                        log.warn("未知的操作类型: {}", type);
                        break;
                }
            } catch (Exception e) {
                log.error("处理数据变更失败: {}", JSON.toJSONString(canalData), e);
            }
        }
    }
    
    /**
     * 处理插入或更新操作
     */
    private void handleInsertOrUpdate(CanalMessage.CanalData canalData, CanalConfig.TableConfig tableConfig) {
        JSONObject data = canalData.getData();
        if (data == null) {
            return;
        }
        
        // 获取主键ID（这里假设主键名为id，可根据实际情况修改）
        Object id = data.get("id");
        if (id == null) {
            log.warn("未找到主键ID，跳过处理");
            return;
        }
        
        // 格式化缓存key
        String cacheKey = cacheService.formatKey(tableConfig.getCacheKey(), id);
        
        // 更新缓存
        cacheService.updateCache(cacheKey, data);
    }
    
    /**
     * 处理删除操作
     */
    private void handleDelete(CanalMessage.CanalData canalData, CanalConfig.TableConfig tableConfig) {
        JSONObject data = canalData.getData();
        if (data == null) {
            return;
        }
        
        // 获取主键ID（这里假设主键名为id，可根据实际情况修改）
        Object id = data.get("id");
        if (id == null) {
            log.warn("未找到主键ID，跳过处理");
            return;
        }
        
        // 格式化缓存key
        String cacheKey = cacheService.formatKey(tableConfig.getCacheKey(), id);
        
        // 删除缓存
        cacheService.deleteCache(cacheKey);
    }
} 