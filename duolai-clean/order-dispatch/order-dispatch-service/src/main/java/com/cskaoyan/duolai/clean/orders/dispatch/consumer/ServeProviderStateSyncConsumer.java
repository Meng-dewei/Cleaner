package com.cskaoyan.duolai.clean.orders.dispatch.consumer;//package com.jzo2o.orders.manager.user;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.canal.converter.CanalConverter;
import com.cskaoyan.duolai.clean.canal.listeners.AbstractCanalRocketHandler;
import com.cskaoyan.duolai.clean.common.constants.EsIndex;
import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import com.cskaoyan.duolai.clean.orders.dispatch.client.ServeProviderApi;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.ServeProviderSyncDO;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.cskaoyan.duolai.clean.user.dto.ServeProviderInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ServeProviderStateSyncConsumer extends AbstractCanalRocketHandler<ServeProviderSyncDO> {

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;


    private DefaultMQPushConsumer consumer;

    @Resource
    ElasticsearchRestTemplate elasticsearchTemplate;


    @Resource
    CanalConverter canalConverter;

    @Resource
    RedissonClient redissonClient;

    @Resource
    ServeProviderApi serveProviderApi;


    @PostConstruct
    public void init() {

        super.init(canalConverter);
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "sync_order_seize_provider_state");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("start sync order dispatch provider state....");
                MessageExt messageExt = msgs.get(0);
                try {
                    parseMsg(messageExt);
                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });


        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.ORDER_SIZE_SERVE_PROVIDER_SYNC_TOPIC, "*");

            // 启动consumer
            consumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }



    @Override
    public void batchSave(List<ServeProviderSyncDO> data) {
        // 1.同步es
        data.forEach(serveProviderSyncDO -> {

            UpdateQuery updateQuery = UpdateQuery.builder(serveProviderSyncDO.getId().toString())
                    .withDocument(Document.parse(JSON.toJSONString(serveProviderSyncDO))) // 使用文档部分更新
                    .build();

            // 增量修改
            elasticsearchTemplate.update(updateQuery, IndexCoordinates.of(EsIndex.SERVE_PROVIDER_INFO));

            // 同步redis中的时间
            Long providerId = serveProviderSyncDO.getId();
            String serveProviderTimeRedisKey = String.format(RedisConstants.RedisKey.SERVE_PROVIDER_TIME, providerId.toString());
            RSet<String> set = redissonClient.getSet(serveProviderTimeRedisKey, StringCodec.INSTANCE);


            // 找到可能取消的服务时间
            List<String> toDeleteServeTime = set.stream()
                    .filter(serveTimeStr -> !serveProviderSyncDO.getServeTimes().contains(Long.parseLong(serveTimeStr)))
                    .collect(Collectors.toList());
            // 删除缓存中待删除的服务时间
            set.removeAll(toDeleteServeTime);


            // 同步接单数
            ServeProviderInfoDTO detail = serveProviderApi.getDetail(serveProviderSyncDO.getId());
            String serveProviderAcceptNumRedisKey = String.format(RedisConstants.RedisKey.SERVE_PROVIDER_ACCEPT_NUM, detail.getCityCode());
            redissonClient.getMap(serveProviderAcceptNumRedisKey, StringCodec.INSTANCE)
                    .put(providerId.toString(), serveProviderSyncDO.getAcceptanceNum());
        });
    }

    @Override
    public void batchDelete(List<Long> ids) {
    }
}
