package com.cskaoyan.duolai.clean.orders.dispatch.consumer;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDispatchService;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
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
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class OrdersDispatchConsumer {


    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;

    private DefaultMQPushConsumer consumer;

    @Resource
    private IOrderDispatchService ordersDispatchService;

    @PostConstruct
    public void init() {
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "dispatch_seize_order");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("receive order dispatch msg,msg={}", JSON.toJSONString(msgs));
                MessageExt messageExt = msgs.get(0);
                try {
                    String str = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                    OrderDispatchDO orderDispatchDO = JSON.parseObject(str, OrderDispatchDO.class);
                    ordersDispatchService.dispatch(orderDispatchDO);
                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });


        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.DISPATCH_ORDERS_TOPIC, "*");

            // 启动consumer
            consumer.start();

            log.info("user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }
}
