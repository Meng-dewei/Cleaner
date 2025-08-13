package com.cskaoyan.duolai.clean.order.dispatch.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("服务取消模型")
public class OrderServeProviderCancelDTO {
    @ApiModelProperty(value = "服务单id",required = true)
    private Long id;

    @ApiModelProperty(value = "取消原因",required = true)
    private String cancelReason;

}
