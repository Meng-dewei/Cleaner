package com.cskaoyan.duolai.clean.orders.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCreateService;
import com.cskaoyan.duolai.clean.pay.enums.PayStateEnum;
import com.cskaoyan.duolai.clean.pay.msg.PayStatusMsg;
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
import java.util.List;
import java.util.stream.Collectors;

import static com.cskaoyan.duolai.clean.orders.constants.OrderConstants.PRODUCT_APP_ID;

/**
 * 接收支付结果
 **/
@Slf4j
@Component
public class TradeStatusConsumer {
    @Resource
    private IOrdersCreateService ordersCreateService;

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;


    private DefaultMQPushConsumer consumer;


    @PostConstruct
    public void init() {
        log.info("TradeStatusConsumer init ...");
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "_trade_status");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("接收到支付结果状态的消息 ({})-> {}", MqTopicConstant.PAY_STATUS_TOPIC, JSON.toJSONString(msgs));
                MessageExt messageExt = msgs.get(0);
                try {
                    listenTradeUpdatePayStatusMsg(new String(messageExt.getBody()));
                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });


        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.PAY_STATUS_TOPIC, "*");

            // 启动consumer
            consumer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }


    /**
     * 更新支付结果
     * 支付成功
     *
     * @param msg 消息
     */
    public void listenTradeUpdatePayStatusMsg(String msg) {
        log.info("接收到支付结果状态的消息 ({})-> {}", MqTopicConstant.PAY_STATUS_TOPIC, msg);
        List<PayStatusMsg> payStatusMsgList = JSON.parseArray(msg, PayStatusMsg.class);

        // 只处理家政服务的订单且是支付成功的
        List<PayStatusMsg> msgList = payStatusMsgList.stream()
                .filter(v -> v.getStatusCode().equals(PayStateEnum.YJS.getCode())
                        && PRODUCT_APP_ID.equals(v.getProductAppId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(msgList)) {
            return;
        }

        //修改订单状态
        msgList.forEach(m -> ordersCreateService.paySuccess(m));
    }
}
