package com.cskaoyan.duolai.clean.orders.dispatch.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  “”
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("服务单状态的数量")
public class OrderServeStatusDTO {
    @ApiModelProperty(required = true, value = "未开始服务")
    private Long noServed;
    @ApiModelProperty(required = true, value = "服务中，待服务完成")
    private Long serving;

    public static OrderServeStatusDTO empty() {
        return new OrderServeStatusDTO(0L,0L);
    }
}
