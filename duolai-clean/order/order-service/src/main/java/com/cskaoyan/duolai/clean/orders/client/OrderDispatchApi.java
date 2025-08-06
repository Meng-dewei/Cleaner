package com.cskaoyan.duolai.clean.orders.client;

//import com.cskaoyan.duolai.clean.order.dispatch.param.OrderSeizeParam;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-dispatch", contextId = "order-dispatch-order-dispatch", path = "/order-dispatch/inner")
public interface OrderDispatchApi {

    /**
     * 机器抢单
     *
     * @param
     */
//    @PostMapping("/dispatch")
//    void machineSeize(@RequestBody OrderSeizeParam orderSeizeParam);

    @PutMapping("division")
    void orderDivision(@RequestBody OrderParam orderParam);

    @DeleteMapping("/dispatch/clean/pool")
    void clearSeizeDispatchPool(@RequestParam("id") Long orderId, @RequestParam("cityCode") String cityCode );


    @PutMapping("/dispatch/no-serve/cancel")
    void noServeCancelByUserAndOperation(@RequestParam("id") Long orderId);
    @PutMapping("/dispatch/serving/cancel")
    void servingCancelByUserAndOperation(@RequestParam("id")Long orderId);
}
