package com.cskaoyan.duolai.clean.orders.dispatch.consumer;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.canal.converter.CanalConverter;
import com.cskaoyan.duolai.clean.canal.listeners.AbstractCanalRocketHandler;
import com.cskaoyan.duolai.clean.common.model.Location;
import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import com.cskaoyan.duolai.clean.common.model.OrdersSeizeInfo;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderSeizeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.repository.OrderSeizeRepository;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OrdersSeizePoolSyncConsumer extends AbstractCanalRocketHandler<OrderSeizeDO> {

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;



    @Resource
    private RedissonClient redissonClient;


    private DefaultMQPushConsumer consumer;

    @Resource
    private OrderSeizeRepository orderSeizeRepository;


    @Resource
    CanalConverter canalConverter;

    @Resource
    OrderSeizeConverter orderSeizeConverter;


    @PostConstruct
    public void init() {

        super.init(canalConverter);
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "sync_order_seize_pool");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
               log.info("start sync order seize....");
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
            consumer.subscribe(MqTopicConstant.ORDERS_SEIZE_POOL_SYNC_TOPIC, "*");

            // 启动consumer
            consumer.start();
            log.info("order seize pool sync user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void batchSave(List<OrderSeizeDO> orderSeizeDOS) {
        // 1.es中添加抢单信息
        List<OrdersSeizeInfo> ordersSeizeInfos = orderSeizeDOS.stream().map(ordersSeize -> {
            OrdersSeizeInfo ordersSeizeInfo = orderSeizeConverter.ordersSeizeToOrdersSeizeInfo(ordersSeize);
            ordersSeizeInfo.setLocation(new Location(ordersSeize.getLon(), ordersSeize.getLat()));
            return ordersSeizeInfo;
        }).collect(Collectors.toList());

        // 将抢单数据写入到ES中, 基于orderSeizeRepository完成，自己写!!!;
        orderSeizeRepository.saveAll(ordersSeizeInfos);

        // 2.写入库存
        ordersSeizeInfos.stream().forEach(ordersSeizeInfo -> {
            String redisKey = String.format(RedisConstants.RedisKey.ORDERS_RESOURCE_STOCK, ordersSeizeInfo.getCityCode());
            // 库存默认1
            RMap<String, String> map = redissonClient.getMap(redisKey, StringCodec.INSTANCE);

            // 向redis中放入抢单库存 key为抢单id(即订单id)，value为"1"
            map.put(ordersSeizeInfo.getId().toString(), "1");
        });
    }

    @Override
    public void batchDelete(List<Long> ids) {
        log.info("抢单删除开始，删除数量:{},开始id：{}，结束id:{}", CollUtils.size(ids), CollUtils.getFirst(ids), CollUtils.getLast(ids));
        orderSeizeRepository.deleteAllById(ids);
        log.info("抢单删除结束，删除数量:{},开始id：{}，结束id:{}", CollUtils.size(ids), CollUtils.getFirst(ids), CollUtils.getLast(ids));

    }
}
