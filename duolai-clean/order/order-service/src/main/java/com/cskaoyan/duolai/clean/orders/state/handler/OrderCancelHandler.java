package com.cskaoyan.duolai.clean.orders.state.handler;

import cn.hutool.db.DbRuntimeException;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.enums.StatusChangeEvent;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCanceledService;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCommonService;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import com.cskaoyan.duolai.clean.orders.state.core.StatusChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 未支付时订单取消处理器  未支付 ——> 已取消
 **/
@Slf4j
@Component("order_cancel")
public class OrderCancelHandler implements StatusChangeHandler<OrderSnapshotDTO> {
    @Resource
    private IOrdersCommonService ordersService;


    /**
     *
     *
     * @param bizId   业务id
     * @param bizSnapshot 快照
     */
    @Override
    public void handler(String bizId,  OrderSnapshotDTO bizSnapshot) {
        log.info("订单取消事件处理逻辑开始，订单号：{}", bizId);
        // 修改订单状态
        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder()
                .id(Long.valueOf(bizId))
                .originStatus(OrderStatusEnum.NO_PAY.getStatus())
                .targetStatus(OrderStatusEnum.CANCELED.getStatus())
                .build();
        int result = ordersService.updateStatus(orderUpdateDTO);
        if (result <= 0) {
            throw new DbRuntimeException("订单取消事件处理失败");
        }
    }
}
