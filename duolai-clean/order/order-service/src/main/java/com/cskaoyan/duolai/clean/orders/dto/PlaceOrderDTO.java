package com.cskaoyan.duolai.clean.orders.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel("下单响应信息")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderDTO {
    @ApiModelProperty("订单id")
    private Long id;
}
