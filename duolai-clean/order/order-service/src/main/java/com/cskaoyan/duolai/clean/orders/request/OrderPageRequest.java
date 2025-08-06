package com.cskaoyan.duolai.clean.orders.request;

import com.cskaoyan.duolai.clean.common.model.dto.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单分页查询请求
 **/
@Data
@ApiModel("订单分页查询请求")
public class OrderPageRequest extends PageRequest {
    /**
     * 用户id
     */
    @ApiModelProperty("用户id")
    private Long userId;

    /**
     * 订单id
     */
    @ApiModelProperty("订单id")
    private Long id;

    /**
     * 客户电话
     */
    @ApiModelProperty("客户电话")
    private String contactsPhone;

    /**
     * 最小创建时间
     */
    @ApiModelProperty("最小创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime minCreateTime;

    /**
     * 最大创建时间
     */
    @ApiModelProperty("最大创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maxCreateTime;

    /**
     * 订单id列表
     */
    @ApiModelProperty("订单id列表")
    private List<Long> ordersIdList;
}
