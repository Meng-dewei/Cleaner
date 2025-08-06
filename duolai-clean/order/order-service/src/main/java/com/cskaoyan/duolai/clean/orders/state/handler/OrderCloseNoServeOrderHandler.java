package com.cskaoyan.duolai.clean.orders.state.handler;

import cn.hutool.db.DbRuntimeException;
import com.cskaoyan.duolai.clean.orders.enums.StatusChangeEvent;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCommonService;
import com.cskaoyan.duolai.clean.orders.enums.OrderRefundStatusEnum;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import com.cskaoyan.duolai.clean.orders.state.core.StatusChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 待服务订单关闭处理器
 **/
@Slf4j
@Component("order_close_no_serve_order")
public class OrderCloseNoServeOrderHandler implements StatusChangeHandler<OrderSnapshotDTO> {
    @Resource
    private IOrdersCommonService ordersService;

    /**
     * 待服务订单关闭处理逻辑
     *
     * @param bizId   业务id
     * @param bizSnapshot 快照
     */
    @Override
    public void handler(String bizId,  OrderSnapshotDTO bizSnapshot) {
        log.info("待服务订单关闭事件处理逻辑开始，订单号：{}", bizId);
        // 修改订单状态
        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder().id(Long.valueOf(bizId))
                .originStatus(OrderStatusEnum.NO_SERVE.getStatus())
                .targetStatus(OrderStatusEnum.CLOSED.getStatus())
                .refundStatus(OrderRefundStatusEnum.REFUNDING.getStatus())//退款状态为退款中
                .build();
        int result = ordersService.updateStatus(orderUpdateDTO);
        if (result <= 0) {
            throw new DbRuntimeException("待服务订单关闭事件处理失败");
        }
    }
}
