package com.cskaoyan.duolai.clean.order.dispatch.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("抢单模型")
public class OrderSeizeParam {
    @ApiModelProperty("抢单id")
    private Long seizeId;
    @ApiModelProperty("服务人员或机构id")
    private Long serveProviderId;
    @ApiModelProperty("服务人员或机构类型，2：服务人员，3：机构")
    private Integer serveProviderType;
}
