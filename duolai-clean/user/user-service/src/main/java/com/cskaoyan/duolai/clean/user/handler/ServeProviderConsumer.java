package com.cskaoyan.duolai.clean.user.handler;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.canal.converter.CanalConverter;
import com.cskaoyan.duolai.clean.canal.listeners.AbstractCanalRocketHandler;
import com.cskaoyan.duolai.clean.common.model.Location;
import com.cskaoyan.duolai.clean.common.model.ServeProviderInfo;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.cskaoyan.duolai.clean.user.converter.ServeProviderConverter;
import com.cskaoyan.duolai.clean.user.dao.entity.ServeProviderSyncDO;
import com.cskaoyan.duolai.clean.user.dao.repository.ServeProviderRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
@Slf4j
public class ServeProviderConsumer extends AbstractCanalRocketHandler<ServeProviderSyncDO> {

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;

    @Resource
    CanalConverter canalConverter;


    private DefaultMQPushConsumer consumer;

    @Resource
    private ServeProviderConverter serveProviderConverter;

    @Resource
    ServeProviderRepository serveProviderRepository;

    @PostConstruct
    public void init() {

        super.init(canalConverter);
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "sync_serve_provider");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("receive serve_provider msg,msg={}", JSON.toJSONString(msgs));
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
            consumer.subscribe(MqTopicConstant.USER_SERVE_PROVIDER_SYNC_TOPIC, "*");

            // 启动consumer
            consumer.start();

            log.info("user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void batchSave(List<ServeProviderSyncDO> data) {

        List<ServeProviderInfo> serveProviderInfos = data.stream().map(serveProviderSyncDO -> {
            ServeProviderInfo serveProviderInfo = serveProviderConverter
                    .serveProviderSyncDOToServeProviderInfo(serveProviderSyncDO);
            Location location = new Location();
            if (serveProviderSyncDO.getLat() != null && serveProviderSyncDO.getLon() != null) {
                location.setLat(serveProviderSyncDO.getLat());
                location.setLon(serveProviderSyncDO.getLon());
                serveProviderInfo.setLocation(location);
            }
            return serveProviderInfo;
        }).collect(Collectors.toList());

        log.debug("serveProviderInfos : {}", serveProviderInfos);

        //ES 保存数据到serveProviderRepository.saveAll(serveProviderInfos);
        serveProviderRepository.saveAll(serveProviderInfos);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        serveProviderRepository.deleteAllById(ids);
    }
}
