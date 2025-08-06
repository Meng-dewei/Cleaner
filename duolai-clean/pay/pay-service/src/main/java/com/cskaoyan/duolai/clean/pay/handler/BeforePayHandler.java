package com.cskaoyan.duolai.clean.pay.handler;

import com.cskaoyan.duolai.clean.pay.model.entity.RefundRecord;
import com.cskaoyan.duolai.clean.pay.model.entity.Trading;

import java.math.BigDecimal;

/**
 * 交易前置处理接口
 */
public interface BeforePayHandler {



    /***
     * 交易单参数校验
     * @param tradingEntity 交易订单
     * @return 是否符合要求
     */
    void checkCreateTrading(Trading tradingEntity);

    /***
     * QueryTrading交易单参数校验
     * @param trading 交易订单
     */
    void checkQueryTrading(Trading trading);


    /***
     * RefundTrading退款交易单参数校验
     * @param trading 交易订单
     * @param refundAmount 退款金额
     */
    void checkRefundTrading(Trading trading,BigDecimal refundAmount);


    /***
     * QueryRefundTrading交易单参数校验
     * @param refundRecord 退款记录
     */
    void checkQueryRefundTrading(RefundRecord refundRecord);

    /***
     * CloseTrading交易单参数校验
     * @param trading 交易订单
     */
    void checkCloseTrading(Trading trading);
}
