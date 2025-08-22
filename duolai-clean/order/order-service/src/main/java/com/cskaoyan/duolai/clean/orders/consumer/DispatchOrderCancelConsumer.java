package com.cskaoyan.duolai.clean.orders.consumer;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.common.constants.UserType;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersMapper;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
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
public class DispatchOrderCancelConsumer {


    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;


    @Resource
    IOrdersManagerService ordersManagerService;


    private DefaultMQPushConsumer consumer;

    @Resource
    private OrdersMapper ordersMapper;


    @Resource
    private OrderConverter orderConverter;

    @PostConstruct
    public void init() {
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "_order_cancel");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                log.info("receive order cancel msg,msg={}", JSON.toJSONString(msgs));
                MessageExt messageExt = msgs.get(0);
                try {
                    String str = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                    List<Long> orderIds = JSON.parseArray(str, Long.class);
                    List<OrdersDO> ordersDOs = ordersMapper.selectBatchIds(orderIds);
                    ordersDOs.forEach(ordersDO -> {
                        OrderCancelDTO orderCancelDTO = orderConverter.ordersDOToOrderCancelDTO(ordersDO);
                        orderCancelDTO.setCurrentUserType(UserType.SYSTEM);
                        orderCancelDTO.setCancelReason("订单抢单超时取消");
                        ordersManagerService.cancel(orderCancelDTO);
                    });

                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });


        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.ORDERS_CANCEL_TOPIC, "*");

            // 启动consumer
            consumer.start();

            log.info("user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

    }
}
