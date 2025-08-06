package com.cskaoyan.duolai.clean.orders.client;

import com.cskaoyan.duolai.clean.pay.dto.RefundDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;


@FeignClient(name = "pay", contextId = "pay-refund", path = "/pay/inner/refund-record")
public interface RefundRecordApi {

    /***
     * 统一收单交易退款接口
     * 当交易发生之后一段时间内，由于买家或者卖家的原因需要退款时，卖家可以通过退款接口将支付款退还给买家，
     * 将在收到退款请求并且验证成功之后，按照退款规则将支付款按原路退到买家帐号上。
     * @param tradingOrderNo 支付单号
     * @param refundAmount 退款金额
     * @return
     */
    @PostMapping("refund")
    RefundDTO refundTrading(@RequestParam("tradingOrderNo") Long tradingOrderNo,
                            @RequestParam("refundAmount") BigDecimal refundAmount);
}
