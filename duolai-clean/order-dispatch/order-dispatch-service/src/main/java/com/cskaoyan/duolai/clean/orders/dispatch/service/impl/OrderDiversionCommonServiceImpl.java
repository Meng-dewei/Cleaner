package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;


import com.cskaoyan.duolai.clean.common.utils.BooleanUtils;
import com.cskaoyan.duolai.clean.common.utils.DateUtils;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.housekeeping.dto.ConfigRegionDTO;
import com.cskaoyan.duolai.clean.housekeeping.dto.ServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import com.cskaoyan.duolai.clean.orders.dispatch.client.ServeApi;
import com.cskaoyan.duolai.clean.orders.dispatch.client.RegionApi;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersDispatchMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersSeizeMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDiversionCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;

@Service
@Slf4j
public class OrderDiversionCommonServiceImpl implements IOrderDiversionCommonService {

    @Resource
    private RegionApi regionApi;

    @Resource
    private ServeApi serveApi;

    @Resource
    private OrderDiversionCommonServiceImpl owner;

    @Resource
    private OrdersSeizeMapper ordersSeizeMapper;
    @Resource
    private OrdersDispatchMapper ordersDispatchMapper;

    @Override
    public void diversion(OrderParam orderParam) {
        log.debug("订单分流，id:{}", orderParam.getId());
        // 1.当前时间已超过服务预约时间则不再分流
        if (orderParam.getServeStartTime().compareTo(DateUtils.now()) < 0) {
            log.debug("订单{}当前时间已超过服务预约时间则不再分流", orderParam.getId());
            return;
        }
        // 调用家政服务
        ConfigRegionDTO configRegion = regionApi.findConfigRegionByCityCode(orderParam.getCityCode());
        ServeDetailDTO serveDetailDTO = serveApi.findById(orderParam.getServeId());
        //订单分流数据存储
        owner.doDiversion(orderParam,configRegion, serveDetailDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doDiversion(OrderParam orderParam, ConfigRegionDTO configRegion, ServeDetailDTO serveDetailDTO) {
        //流间隔（单位分钟），即当前时间与服务预计开始时间的间隔
        Integer diversionInterval = configRegion.getDiversionInterval();

        //当前时间与服务预约时间的间隔
        Duration between = DateUtils.between(DateUtils.now(), orderParam.getServeStartTime());
        //服务类型名称
        String serveTypeName = ObjectUtils.get(serveDetailDTO, ServeDetailDTO::getServeTypeName);
        //服务类型id
        Long serveTypeId = ObjectUtils.get(serveDetailDTO, ServeDetailDTO::getServeTypeId);
        //服务项名称
        String serveItemName = ObjectUtils.get(serveDetailDTO, ServeDetailDTO::getServeItemName);
        //服务项图片
        String serveItemImg = ObjectUtils.get(serveDetailDTO, ServeDetailDTO::getServeItemImg);
        //用于排序,服务预约时间戳加订单号后5位
        long sortBy = DateUtils.toEpochMilli(orderParam.getServeStartTime()) + orderParam.getId() % 100000;
        OrderSeizeDO orderSeizeDO = OrderSeizeDO.builder()
                .id(orderParam.getId())
                .ordersAmount(orderParam.getRealPayAmount())
                .cityCode(orderParam.getCityCode())
                .serveTypeId(serveTypeId)
                .serveTypeName(serveTypeName)
                .serveItemId(orderParam.getServeItemId())
                .serveItemName(serveItemName)
                .serveItemImg(serveItemImg)
                .ordersAmount(orderParam.getRealPayAmount())
                .serveStartTime(orderParam.getServeStartTime())
                .serveAddress(orderParam.getServeAddress())
                .lon(orderParam.getLon())
                .lat(orderParam.getLat())
                .paySuccessTime(orderParam.getPayTime())
                .sortBy(sortBy)
                .isTimeOut(BooleanUtils.toInt(between.toMinutes() < diversionInterval))
                .purNum(orderParam.getPurNum()).build();

        // 保存抢单记录到抢单表
        ordersSeizeMapper.insert(orderSeizeDO);

        //当前时间与服务预约时间的间隔 小于指定间隔则插入派单表
        if (between.toMinutes() < diversionInterval) {
            OrderDispatchDO orderDispatchDO = OrderDispatchDO.builder()
                    .id(orderParam.getId())
                    .ordersAmount(orderParam.getRealPayAmount())
                    .cityCode(orderParam.getCityCode())
                    .serveTypeId(serveTypeId)
                    .serveTypeName(serveTypeName)
                    .serveItemId(orderParam.getServeItemId())
                    .serveItemName(serveItemName)
                    .serveItemImg(serveItemImg)
                    .ordersAmount(orderParam.getRealPayAmount())
                    .serveStartTime(orderParam.getServeStartTime())
                    .serveAddress(orderParam.getServeAddress())
                    .lon(orderParam.getLon())
                    .lat(orderParam.getLat())
                    // 派单中
                    .status(1)
                    .purNum(orderParam.getPurNum()).build();

            // 保存派单记录到派单表
             ordersDispatchMapper.insert(orderDispatchDO);

        }
    }


}
