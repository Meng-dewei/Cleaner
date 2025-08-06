package com.cskaoyan.duolai.clean.orders.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * 订单支付响应体
 **/
@Data
@ApiModel("订单支付响应体")
public class OrdersPayDTO {

    @ApiModelProperty(value = "二维码base64数据")
    private String qrCode;
    @ApiModelProperty(value = "业务系统订单号")
    private Long productOrderNo;
    @ApiModelProperty(value = "交易系统订单号【对于三方来说：商户订单】")
    private Long tradingOrderNo;
    @ApiModelProperty(value = "支付渠道【支付宝、微信、现金、免单挂账】")
    private String tradingChannel;
    @ApiModelProperty(value = "支付状态 待支付或支付成功")
    private int payStatus;
}
