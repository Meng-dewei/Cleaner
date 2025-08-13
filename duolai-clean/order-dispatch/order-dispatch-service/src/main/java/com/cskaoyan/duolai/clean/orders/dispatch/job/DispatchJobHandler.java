package com.cskaoyan.duolai.clean.orders.dispatch.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersDispatchMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.rocketmq.client.RocketMQClient;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 派单分发xxl-job定时任务
 */
@Component
@Slf4j
public class DispatchJobHandler {

    @Resource
    private OrdersDispatchMapper ordersDispatchMapper;


    @Resource
    private RocketMQClient rocketMQClient;

    /**
     * 派单分发任务，每3分钟执行一次
     */
    @XxlJob("dispatch")
    public void dispatchDistributeJob() {

        LambdaQueryWrapper<OrderDispatchDO> wrapper = Wrappers.lambdaQuery(OrderDispatchDO.class)
                .orderByAsc(OrderDispatchDO::getServeStartTime)
                .eq(OrderDispatchDO::getStatus, 1)
                .last("limit " + 100);


        List<OrderDispatchDO> orderDispatchDOS =  ordersDispatchMapper.selectList(wrapper);

        if (CollUtils.isEmpty(orderDispatchDOS)) {
            log.debug("当前没有可以派单数据");
            return;
        }

        orderDispatchDOS.forEach(ordersDispatchDO -> {
            if (ordersDispatchDO.getServeStartTime().isBefore(LocalDateTime.now())) {
                // 已经超过了服务时间了，修改状态为派单失败
                OrderDispatchDO toUpdate = new OrderDispatchDO();
                // 派单失败
                toUpdate.setStatus(2);
                toUpdate.setId(ordersDispatchDO.getId());
                ordersDispatchMapper.updateById(toUpdate);
                return;
            }

            // 异步完成派单
            // 发送派单消息进行派单 rocketMQClient.sendMessage(MqTopicConstant.DISPATCH_ORDERS_TOPIC, ordersDispatchDO);

        });
    }


}
