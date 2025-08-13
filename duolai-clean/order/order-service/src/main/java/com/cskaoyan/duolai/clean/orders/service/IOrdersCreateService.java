package com.cskaoyan.duolai.clean.orders.service;

import com.baomidou.mybatisplus.extension.service.IService;
//import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.request.OrdersPayCommand;
import com.cskaoyan.duolai.clean.orders.request.PlaceOrderCommand;
import com.cskaoyan.duolai.clean.orders.dto.OrdersPayDTO;
import com.cskaoyan.duolai.clean.orders.dto.PlaceOrderDTO;
import com.cskaoyan.duolai.clean.pay.msg.PayStatusMsg;

import java.util.List;

/**
 * <p>
 * 下单服务类
 * </p>
 */
public interface IOrdersCreateService extends IService<OrdersDO> {

    /**
     * 获取可用优惠券
     *
     * @param serveId 服务id
     * @param purNum  购买数量
     * @return 可用优惠券列表
     */
    List<AvailableCouponsDTO> getAvailableCoupons(Long serveId, Integer purNum);

    /**
     * 下单
     *
     * @param placeOrderCommand
     * @return
     */
    PlaceOrderDTO placeOrder(PlaceOrderCommand placeOrderCommand);

    /**
     * 更新支付状态
     *
     * @param id        订单id
     * @param payStatus 支付状态
     */
    Boolean updatePayStatus(Long id, Integer payStatus);

    /**
     * 更新退款状态
     *
     * @param id           订单id
     * @param refundStatus 退款状态
     * @param refundId     第三方支付的退款单号
     * @param refundNo     支付服务退款单号
     */
    Boolean updateRefundStatus(Long id, Integer refundStatus, String refundId, Long refundNo);



    /**
     * 生成订单
     *
     * @param ordersDO
     */
    void add(OrdersDO ordersDO);

    /**
     * 生成订单 使用优惠券
     *
     * @param ordersDO   订单信息
     * @param couponId 优惠券id
     */
    void addWithCoupon(OrdersDO ordersDO, Long couponId);

    /**
     * 支付成功， 其他信息暂且不填
     *
     * @param payStatusMsg 交易状态消息
     */
    void paySuccess(PayStatusMsg payStatusMsg);

    /**
     * 订单支付
     *
     * @param id              订单id
     * @param ordersPayCommand 订单支付请求体
     * @return 订单支付响应体
     */
    OrdersPayDTO pay(Long id, OrdersPayCommand ordersPayCommand);



    /**
     * 请求支付服务查询支付结果
     *
     * @param id 订单id
     * @return 订单支付响应体
     */
    int getPayResult(Long id);

    /**
     * 查询超时订单id列表
     *
     * @param count 数量
     * @return 订单id列表
     */
    List<OrdersDO> queryOverTimePayOrdersListByCount(Integer count);


}
