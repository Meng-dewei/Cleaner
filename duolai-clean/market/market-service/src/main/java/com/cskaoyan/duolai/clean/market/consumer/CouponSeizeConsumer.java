package com.cskaoyan.duolai.clean.market.consumer;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponResultDTO;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
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
public class CouponSeizeConsumer{

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;

    @Resource
    ICouponService couponService;


    private DefaultMQPushConsumer consumer;

    @PostConstruct
    public void init() {
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "_seize_coupon");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("receive dispatch coupon msg,msg={}", JSON.toJSONString(msgs));
                MessageExt messageExt = msgs.get(0);
                try {
                    String str = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                    SeizeCouponResultDTO seizeCouponResultDTO = JSON.parseObject(str, SeizeCouponResultDTO.class);
                    couponService.syncCouponSeizeInfo(seizeCouponResultDTO);
                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });


        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.COUPON_SEIZE_TOPIC, "*");

            // 启动consumer
            consumer.start();

            log.info("user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }

}
