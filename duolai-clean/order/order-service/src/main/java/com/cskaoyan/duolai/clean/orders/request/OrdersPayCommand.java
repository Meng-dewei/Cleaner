package com.cskaoyan.duolai.clean.orders.request;

import com.cskaoyan.duolai.clean.pay.enums.PayChannelEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 订单支付请求体
 **/
@Data
@ApiModel("订单支付请求体")
public class OrdersPayCommand {
    @ApiModelProperty(value = "支付渠道", required = true)
    private PayChannelEnum tradingChannel;
}
