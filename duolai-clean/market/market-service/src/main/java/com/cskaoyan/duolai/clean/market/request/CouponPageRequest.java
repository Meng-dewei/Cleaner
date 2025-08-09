package com.cskaoyan.duolai.clean.market.request;

import com.cskaoyan.duolai.clean.common.model.dto.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel("运营端优惠券查询模型")
public class CouponPageRequest extends PageRequest {
    @ApiModelProperty(value = "活动id",required = true)
    private Long activityId;
}
