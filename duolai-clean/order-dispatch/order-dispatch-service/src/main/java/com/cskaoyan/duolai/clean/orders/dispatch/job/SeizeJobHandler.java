package com.cskaoyan.duolai.clean.orders.dispatch.job;


import com.cskaoyan.duolai.clean.common.utils.CollUtils;
import com.cskaoyan.duolai.clean.housekeeping.dto.ConfigRegionDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.client.RegionApi;
import com.cskaoyan.duolai.clean.orders.dispatch.converter.OrderSeizeConverter;
import com.cskaoyan.duolai.clean.orders.dispatch.config.RedissonCancelSeizeOrderLuaHandler;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.repository.OrderSeizeRepository;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDispatchService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import com.cskaoyan.duolai.clean.rocketmq.client.RocketMQClient;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 抢单xxl-job任务
 */
@Component
@Slf4j
public class SeizeJobHandler {

    @Resource
    private IOrderSeizeService ordersSeizeService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RegionApi regionApi;

    @Resource
    private SeizeJobHandler owner;

    @Resource
    private IOrderDispatchService ordersDispatchService;

    @Resource
    private OrderSeizeRepository orderSeizeRepository;

    @Resource
    private IOrderServeManagerService ordersServeManagerService;


    @Resource
    private RedissonCancelSeizeOrderLuaHandler redissonCancelSeizeOrderLuaHandler;

    @Resource
    private RocketMQClient rocketMQClient;

    @Resource
    OrderSeizeConverter orderSeizeConverter;


    /**
     * 当前时间距离服务预约时间间隔小于配置值时进入派单池，每5分钟执行一次
     */
    @XxlJob("seizeTimeoutIntoDispatchPoolJob")
    public void seizeOrdersIntoDispatchPoolJob() {
        // 调用家政服务，获取区域对应的调度配置类
        List<ConfigRegionDTO> configRegionDTOS = regionApi.findAll();
        for (ConfigRegionDTO configRegionDTO : configRegionDTOS) {
            try {
                //传入配置的下单时间距离服务预约时间间隔
                owner.queryNeedToDispatchSeizeOrders(configRegionDTO.getCityCode(), configRegionDTO.getDiversionInterval());
            } catch (Exception e) {
                log.error("抢单订单超时处理异常，e:", e);
            }
        }
    }

    /**
     *当前时间距离服务预约时间间隔小于配置值时进入派单池，以城市为单位进行处理
     * @param cityCode
     * @param timeoutInterval
     */
    @Transactional(rollbackFor = Exception.class)
    public void queryNeedToDispatchSeizeOrders(String cityCode, Integer timeoutInterval) {
        // 1.查询满足条件的且未处理的抢单列表
        List<OrderSeizeDO> orderSeizeDOS = ordersSeizeService.queryNeedToDispatchSeizeOrders(cityCode, timeoutInterval);
        if (CollUtils.isEmpty(orderSeizeDOS)) {
            return;
        }

        // 2.修改抢单超时标记并，派单
        List<OrderDispatchDO> orderDispatchDOS = orderSeizeConverter.ordersSeizeDOsToOrdersDispatchDOs(orderSeizeDOS);
        orderDispatchDOS.forEach(ordersDispatchDO -> {
            // 设置状态为派单中
            ordersDispatchDO.setStatus(1);
        });
        //3.指定同步到派单池
        ordersDispatchService.saveOrUpdateBatch(orderDispatchDOS,100);
    }

    /**
     * 到达服务预约时间，终止抢单，每分钟执行一次
     */
    @XxlJob("arriveServeStartTimeStopSeizeJob")
    public void arriveServeStartTimeStopSeizeJob() {
        // 1.到达服务预约时间的抢单信息
        List<OrderSeizeDO> orderSeizeDOS = ordersSeizeService.queryArriveServeStartTimeSeizeOrder();
        if (CollUtils.isEmpty(orderSeizeDOS)) {
            return;
        }

        List<Long> ids = new ArrayList<>();
        orderSeizeDOS.forEach(ordersSeizeDO -> {
            Long result = redissonCancelSeizeOrderLuaHandler.cancelOrderSeize(ordersSeizeDO.getId(), ordersSeizeDO.getCityCode());
            if (result == -1 || result == -2) {
                // 已取消或者已被抢单或者派单
                return;
            }
            ids.add(ordersSeizeDO.getId());
        });
        if (CollUtils.isNotEmpty(ids)) {
            //批量删除抢单列表
            ordersSeizeService.batchTimeout(ids);
            // 删除es中的抢单记录
            orderSeizeRepository.deleteAllById(ids);
        }

        // 发送给用户订单服务，取消订单消息 rocketMQClient.sendMessage(MqTopicConstant.ORDERS_CANCEL_TOPIC, ids);
    }


}
