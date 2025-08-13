package com.cskaoyan.duolai.clean.orders.dispatch.client;

import com.cskaoyan.duolai.clean.common.utils.MyQueryMapEncoder;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderCanceledDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内部接口 - 订单相关接口
 */
@FeignClient(name = "order", contextId = "order-manager", path = "/order/inner", configuration = MyQueryMapEncoder.class)
public interface OrderApi {


    /**
     * 根据id查询订单详情
     *
     * @param id 订单id
     * @return 订单详情
     */
    @GetMapping("/{id}")
    OrderDTO queryById(@PathVariable("id") Long id);

    /**
     * 根据订单id列表批量查询
     *
     * @param ids 订单id列表
     * @return 订单列表
     */
    @GetMapping("queryByIds")
    List<OrderDTO> queryByIds(@RequestParam("ids") List<Long> ids);

    @PutMapping("seize/success/{id}")
    void orderSeizeSuccess(@PathVariable("id") Long id);

    /**
     * 订单对应的服务开始
     * @return 订单列表
     */
    @PutMapping("serve/start/{id}")
    void orderServeStart(@PathVariable("id") Long id);
    /**
     * 订单对应的服务完成
     * @return 订单列表
     */

    /**
     * 订单对应的服务被服务提供者取消
     * @return 订单列表
     */
    @PutMapping("serve/finish/{id}")
    void orderServeFinish(@PathVariable("id") Long id, @RequestBody LocalDateTime localDateTime);


    @GetMapping("canceled/{id}")
    OrderCanceledDTO queryOrderCanceledByOrderId(@PathVariable("id") Long id);
}
