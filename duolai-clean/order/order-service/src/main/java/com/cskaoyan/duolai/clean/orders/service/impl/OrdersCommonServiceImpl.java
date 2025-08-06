package com.cskaoyan.duolai.clean.orders.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCommonService;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersMapper;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 */
@Service
public class OrdersCommonServiceImpl extends ServiceImpl<OrdersMapper, OrdersDO> implements IOrdersCommonService {
    @Override
    public Integer updateStatus(OrderUpdateDTO orderUpdateStatusReqDTO) {
        LambdaUpdateWrapper<OrdersDO> updateWrapper = Wrappers.<OrdersDO>lambdaUpdate()
                .eq(OrdersDO::getId, orderUpdateStatusReqDTO.getId())
                .gt(OrdersDO::getUserId, 0)
                .eq(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getOriginStatus()), OrdersDO::getOrdersStatus,orderUpdateStatusReqDTO.getOriginStatus())
                .set(OrdersDO::getOrdersStatus, orderUpdateStatusReqDTO.getTargetStatus())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getPayStatus()), OrdersDO::getPayStatus,orderUpdateStatusReqDTO.getPayStatus())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getPayTime()), OrdersDO::getPayTime,orderUpdateStatusReqDTO.getPayTime())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getRealServeEndTime()), OrdersDO::getRealServeEndTime,orderUpdateStatusReqDTO.getRealServeEndTime())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getTradingOrderNo()), OrdersDO::getTradingOrderNo,orderUpdateStatusReqDTO.getTradingOrderNo())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getTransactionId()), OrdersDO::getTransactionId,orderUpdateStatusReqDTO.getTransactionId())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getTradingChannel()), OrdersDO::getTradingChannel,orderUpdateStatusReqDTO.getTradingChannel())
                .set(ObjectUtil.isNotNull(orderUpdateStatusReqDTO.getRefundStatus()), OrdersDO::getRefundStatus,orderUpdateStatusReqDTO.getRefundStatus());
        boolean update = super.update(updateWrapper);
        return update?1:0;
    }
}
