package com.cskaoyan.duolai.clean.orders.state.handler;

import cn.hutool.db.DbRuntimeException;
import com.cskaoyan.duolai.clean.orders.enums.StatusChangeEvent;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCommonService;
import com.cskaoyan.duolai.clean.orders.enums.OrderPayStatusEnum;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import com.cskaoyan.duolai.clean.orders.state.core.StatusChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 订单支付成功处理器
 **/
@Slf4j
@Component("order_payed")
public class OrderPayedHandler implements StatusChangeHandler<OrderSnapshotDTO> {
    @Resource
    private IOrdersCommonService ordersService;

    /**
     * 订单支付处理逻辑
     *
     * @param bizId   业务id
     * @param bizSnapshot 快照
     */
    @Override
    public void handler(String bizId, OrderSnapshotDTO bizSnapshot) {
        log.info("支付事件处理逻辑开始，订单号：{}", bizId);
        // 修改订单状态和支付状态
        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder().id(Long.valueOf(bizId))
                .originStatus(OrderStatusEnum.NO_PAY.getStatus())
                .targetStatus(OrderStatusEnum.DISPATCHING.getStatus())
                .payStatus(OrderPayStatusEnum.PAY_SUCCESS.getStatus())
                .payTime(LocalDateTime.now())
                .tradingOrderNo(bizSnapshot.getTradingOrderNo())
                .transactionId(bizSnapshot.getThirdOrderId())
                .tradingChannel(bizSnapshot.getTradingChannel())
                .build();
        int result = ordersService.updateStatus(orderUpdateDTO);
        if (result <= 0) {
            throw new DbRuntimeException("支付事件处理失败");
        }
    }


}
