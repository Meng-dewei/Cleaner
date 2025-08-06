package com.cskaoyan.duolai.clean.orders.request;

import lombok.Data;

/**
 * 订单取消请求
 **/
@Data
public class OrderCancelCommand{
    /**
     * 订单id
     */
    private Long id;

    /**
     * 取消/退款原因
     */
    private String cancelReason;
}
