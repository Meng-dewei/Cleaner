package com.cskaoyan.duolai.clean.market.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class SeizeCouponCommand {
    @ApiModelProperty("活动id")
    private Long id;
}
