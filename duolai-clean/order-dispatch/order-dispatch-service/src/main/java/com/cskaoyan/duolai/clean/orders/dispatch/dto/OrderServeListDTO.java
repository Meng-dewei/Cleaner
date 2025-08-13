package com.cskaoyan.duolai.clean.orders.dispatch.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@ApiModel("服务单列表，无分页")
@NoArgsConstructor
@AllArgsConstructor
public class OrderServeListDTO {

    @ApiModelProperty("服务单列表")
    private List<OrderServeInfoDTO> ordersServes;
}
