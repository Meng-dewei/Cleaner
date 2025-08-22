package com.cskaoyan.duolai.clean.orders.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.cskaoyan.duolai.clean.common.constants.UserType;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.orders.client.RefundRecordApi;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.enums.OrderRefundStatusEnum;
import com.cskaoyan.duolai.clean.orders.enums.OrderStatusEnum;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersRefundDO;
import com.cskaoyan.duolai.clean.orders.properties.OrdersJobProperties;
import com.cskaoyan.duolai.clean.orders.service.IOrdersCreateService;
import com.cskaoyan.duolai.clean.orders.service.IOrdersManagerService;
import com.cskaoyan.duolai.clean.orders.service.IOrdersRefundService;
import com.cskaoyan.duolai.clean.pay.dto.RefundDTO;
import com.cskaoyan.duolai.clean.pay.enums.RefundStatusEnum;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单相关定时任务
 **/
@Slf4j
@Component
public class OrdersHandler {
    @Resource
    private IOrdersCreateService ordersCreateService;
    @Resource
    private IOrdersManagerService ordersManagerService;
    @Resource
    private OrderStateMachine orderStateMachine;
    @Resource
    private RefundRecordApi refundRecordApi;
    @Resource
    private IOrdersRefundService ordersRefundService;
    @Resource
    private OrdersJobProperties ordersJobProperties;

    @Resource
    private OrderConverter orderConverter;

    /**
     * 支付超时取消订单
     * 每分钟执行一次
     */
    @XxlJob(value = "cancelOverTimePayOrder")
    public void cancelOverTimePayOrder() {
        log.info("订单超时取消任务开始执行...");
        //查询支付超时状态订单
        Integer overTimePayOrderCount = ordersJobProperties.getOverTimePayOrderCount();
        ordersCreateService.queryOverTimePayOrdersListByCount(overTimePayOrderCount);
        List<OrdersDO> ordersDOList = ordersCreateService.queryOverTimePayOrdersListByCount(overTimePayOrderCount);
        if (CollUtil.isEmpty(ordersDOList)) {
            XxlJobHelper.log("查询到订单列表为空！");
            return;
        }

        for (OrdersDO ordersDO : ordersDOList) {

            //取消订单
            OrderCancelDTO orderCancelDTO = orderConverter.ordersDOToOrderCancelDTO(ordersDO);
            orderCancelDTO.setCurrentUserType(UserType.SYSTEM);
            orderCancelDTO.setCancelReason("订单超时支付，自动取消");
            ordersManagerService.cancel(orderCancelDTO);
        }
    }


    /**
     * 订单退款异步任务，每10分钟执行一次
     */
    @XxlJob(value = "handleRefundOrders")
    public void handleRefundOrders() {
        log.info("自动退款任务开始执行...");
        ordersRefundService.queryRefundOrderListByCount(ordersJobProperties.getRefundOrderCount());
        //查询退款中订单
        List<OrdersRefundDO> ordersRefundDOList = ordersRefundService.queryRefundOrderListByCount(ordersJobProperties.getRefundOrderCount());
        for (OrdersRefundDO ordersRefundDO : ordersRefundDOList) {
            //请求退款
            requestRefundOrder(ordersRefundDO);
        }
    }


    /**
     * 请求退款
     * @param ordersRefundDO 退款记录
     */
    private void requestRefundOrder(OrdersRefundDO ordersRefundDO){
        //调用第三方进行退款

        try {
            // 服务调用(支付服务)
            RefundDTO refundDTO = refundRecordApi.refundTrading(ordersRefundDO.getTradingOrderNo(), ordersRefundDO.getRealPayAmount());

            if(refundDTO !=null){
                //退款成功，后处理订单相关信息
                refundOrder(ordersRefundDO, refundDTO);
            }
        } catch (Exception e) {
            throw new CommonException("退款失败: " + ordersRefundDO.getId());
        }

    }



    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(OrdersRefundDO ordersRefundDO, RefundDTO refundDTO) {
        //根据响应结果更新支付状态
        int refundStatus = OrderRefundStatusEnum.REFUNDING.getStatus();//退款中
        if (ObjectUtil.equal(RefundStatusEnum.SUCCESS.getCode(), refundDTO.getRefundStatus())) {
            //退款成功
            refundStatus = OrderRefundStatusEnum.REFUND_SUCCESS.getStatus();
        }

        //如果是退款中状态，程序结束
        if (ObjectUtil.equal(refundStatus, OrderRefundStatusEnum.REFUNDING.getStatus())) {
            return;
        }

        // 退款成功
        // 退款成功更新订单中的退款数据
        ordersCreateService.updateRefundStatus(ordersRefundDO.getId(), refundStatus, refundDTO.getRefundId(), refundDTO.getRefundNo());


        //非退款中状态，删除申请退款记录，删除后定时任务不再扫描
        ordersRefundService.removeById(ordersRefundDO.getId());

        //新增快照(因为退款成功并不会修改订单状态，我们只需要新增一个订单快照，在快照中添加退款信息即可，所以调用的是saveSnapshot方法)
        String jsonResult = orderStateMachine.getCurrentSnapshot(ordersRefundDO.getId().toString());
        OrderSnapshotDTO orderSnapshotDTO = JSONUtil.toBean(jsonResult, OrderSnapshotDTO.class);
        //退款状态
        orderSnapshotDTO.setRefundStatus(refundStatus);
        //支付服务的退款单号
        orderSnapshotDTO.setRefundNo(refundDTO.getRefundNo());
        //第三方的退款id
        orderSnapshotDTO.setThirdRefundOrderId(refundDTO.getRefundId());
        // 仅仅保存快照
        orderStateMachine.saveSnapshot(orderSnapshotDTO.getId().toString(), OrderStatusEnum.CLOSED, orderSnapshotDTO);
    }


}
