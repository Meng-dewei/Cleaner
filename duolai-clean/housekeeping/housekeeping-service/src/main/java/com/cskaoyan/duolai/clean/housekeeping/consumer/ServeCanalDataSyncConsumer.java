package com.cskaoyan.duolai.clean.housekeeping.consumer;

import com.alibaba.fastjson.JSON;
import com.cskaoyan.duolai.clean.canal.converter.CanalConverter;
import com.cskaoyan.duolai.clean.canal.listeners.AbstractCanalRocketHandler;
import com.cskaoyan.duolai.clean.common.model.RegionServeInfo;
import com.cskaoyan.duolai.clean.housekeeping.converter.RegionServeConverter;
import com.cskaoyan.duolai.clean.housekeeping.dao.entity.RegionServeSyncDO;
import com.cskaoyan.duolai.clean.housekeeping.dao.repository.RegionServeRepository;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

@Component
@Slf4j
public class ServeCanalDataSyncConsumer extends AbstractCanalRocketHandler<RegionServeSyncDO> {

    @Value("${rocketmq.namesrv.address}")
    String namesrvAddr;

    @Value("${rocketmq.consumer.group.prefix}")
    String consumerGroup;

    @Resource
    CanalConverter canalConverter;


    private DefaultMQPushConsumer consumer;

    @Resource
    RegionServeRepository regionServeRepository;

    @Resource
    RegionServeConverter regionServeConverter;

    @PostConstruct
    public void init() {

        super.init(canalConverter);
        // 创建consumer对象
        consumer = new DefaultMQPushConsumer(consumerGroup + "sync_serve");

        // 设置nameserver地址
        consumer.setNamesrvAddr(namesrvAddr);

        // 设置消息监听器
        consumer.setMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                log.info("receive region_serve_info msg,msg={}", JSON.toJSONString(msgs));
                MessageExt messageExt = msgs.get(0);
                try {
                    parseMsg(messageExt);
                } catch (Exception e) {
                    log.error("parseMsg error,msg={}", JSON.toJSONString(messageExt), e);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }

        });

        try {
            // 订阅主题
            consumer.subscribe(MqTopicConstant.HOUSEKEEPING_TOPIC, "*");

            // 启动consumer
            consumer.start();

            log.info("user start success");
        } catch (MQClientException e) {
            e.printStackTrace();
        }

    }

    /*
          批量更新或者保存数据到ES
     */
    @Override
    public void batchSave(List<RegionServeSyncDO> data) {

        // 转化得到文档对象集合
        List<RegionServeInfo> regionServeInfos = regionServeConverter.syncServeDO2Infos(data);

        // 保存或者全量修改ES中的文档数据
        regionServeRepository.saveAll(regionServeInfos);

    }

    /*
        批量删除ES中的数据
    */
    @Override
    public void batchDelete(List<Long> ids) {

        // 删除文档中的数据
        regionServeRepository.deleteAllById(ids);
    }
}
