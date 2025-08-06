package com.cskaoyan.duolai.clean.orders.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@ApiModel("下单请求信息")
@Data
public class PlaceOrderCommand {
    @ApiModelProperty(value = "服务id",required = true)
    private Long serveId;

    @ApiModelProperty(value = "预约地址id",required = true)
    private Long addressBookId;

    @ApiModelProperty(value = "购买数量",required = false)
    private Integer purNum;

    @ApiModelProperty(value = "预约时间",required = true)
    private LocalDateTime serveStartTime;

    @ApiModelProperty(value = "优惠券id", required = false)
    private Long couponId;
}
