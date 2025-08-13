package com.cskaoyan.duolai.clean.orders.dispatch.request;

import com.cskaoyan.duolai.clean.common.model.dto.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 根据服务人员/机构分页查询服务数据
 **/
@Data
@ApiModel("根据服务人员分页查询服务单相关模型")
public class OrderServePageRequest extends PageRequest {
    /**
     * 用户类型，2：服务人员，3：机构
     */
    @ApiModelProperty(value = "用户类型，2：服务人员，3：机构")
    private Integer userType;

    /**
     * 服务人员/机构id
     */
    @ApiModelProperty(value = "服务人员/机构id")
    private Long serveProviderId;
}
