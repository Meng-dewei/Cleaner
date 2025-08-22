# Canal + RocketMQ 跨表事务缓存同步

本系统使用Canal监听MySQL数据库的binlog变化，并通过RocketMQ将这些变化以事务为单位同步到缓存中。特别支持跨表事务的处理，确保一个事务中对多个表的操作可以被一起消费和处理。

## 核心功能

1. 监听数据库变化并实时同步到缓存
2. 支持**跨表事务**，保证事务完整性
3. 基于RocketMQ的可靠消息传递
4. 支持多种数据库操作(INSERT/UPDATE/DELETE)
5. 可配置的表与缓存键映射

## 系统架构

```
MySQL → Canal Server → RocketMQ → Canal Client → Redis缓存
```

## Canal服务器配置

### canal.properties 关键配置

```properties
# 使用RocketMQ模式
canal.serverMode=rocketmq

# RocketMQ配置
canal.mq.servers=127.0.0.1:9876
canal.mq.batchSize=32768
canal.mq.flatMessage=true

# 关键配置：事务批处理模式
canal.mq.canal.batch.mode=TRANSACTION
# 不拆分事务
canal.mq.transaction.split=false
```

### instance.properties 关键配置

```properties
# 事务内获取数据的模式
canal.instance.getWithinTransaction=true

# 存储模式
canal.instance.memory.batch.mode=TRANSACTION

# 事务配置
canal.instance.transaction.enabled=true
canal.instance.transaction.size=16384
```

## 配置跨表事务支持

要正确处理跨表事务，需要满足以下条件：

1. **所有相关表必须在同一个Canal实例中监听**
   - 通过`canal.instance.filter.regex`配置所有需要监听的表
   - 例如：`xzb_db\\.t_user|xzb_db\\.t_order|xzb_db\\.t_product`

2. **事务设置**
   - `canal.instance.getWithinTransaction=true`
   - `canal.instance.memory.batch.mode=TRANSACTION`
   - `canal.instance.transaction.enabled=true`

3. **RocketMQ设置**
   - `canal.mq.canal.batch.mode=TRANSACTION`
   - `canal.mq.transaction.split=false`
   - `canal.mq.flatMessage=true`

## 客户端消费者

消费者需要能够处理包含多表数据的消息。主要流程：

1. 解析接收到的消息（可能是单表消息或多表事务消息）
2. 对于多表事务消息，按事务ID分组处理
3. 根据每个表的配置更新对应的缓存

## 启动步骤

1. 配置并启动MySQL（需开启binlog）
2. 配置并启动Canal Server
3. 配置并启动RocketMQ
4. 启动Canal Client应用

## 配置示例

### application.yml

```yaml
# Canal配置
canal:
  # 监听哪些表的数据变化，用于缓存同步
  tables:
    - database: xzb_db
      table: t_user
      cache-key: "user:%d"  # %d将被替换为用户ID
    - database: xzb_db
      table: t_product
      cache-key: "product:%d"
    - database: xzb_db
      table: t_order
      cache-key: "order:%d"
  topic: canal-topic
```

## 故障排查

如果遇到跨表事务未能在一个消息中接收的问题，请检查：

1. Canal服务器的batch模式是否设置为TRANSACTION
2. 是否启用了`getWithinTransaction=true`
3. 相关表是否都配置在同一个Canal实例中
4. RocketMQ的batch size是否足够大，能容纳完整事务

## 性能调优

1. 调整`canal.instance.transaction.size`以适应事务大小
2. 调整`canal.mq.batchSize`以优化消息传输
3. 调整消费者的线程池大小，提高并发处理能力 