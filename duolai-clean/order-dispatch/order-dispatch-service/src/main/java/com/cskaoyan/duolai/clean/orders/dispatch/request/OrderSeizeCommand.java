package com.cskaoyan.duolai.clean.orders.dispatch.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("抢单模型")
public class OrderSeizeCommand {
    /**
     * 抢单id
     */
    @ApiModelProperty("抢单id")
    private Long id;
}
