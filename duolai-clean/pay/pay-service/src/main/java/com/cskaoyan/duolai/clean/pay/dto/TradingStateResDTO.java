package com.cskaoyan.duolai.clean.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易单状态响应数据
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("交易单状态响应数据")
public class TradingStateResDTO {

    @ApiModelProperty(value = "交易单状态，2：付款中，4：已结算")
    private Integer tradingState;
}
