package com.cskaoyan.duolai.clean.orders.dispatch.controller.inner;

import com.cskaoyan.duolai.clean.order.dispatch.param.OrderSeizeParam;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.ServeStatusEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDiversionCommonService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.ISeizeDispatchService;
import com.cskaoyan.duolai.clean.orders.model.param.OrderParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Api(tags = "抢单内部相关接口")
@Slf4j
@RequestMapping("/inner")
public class OrderDivisionInnerController {

    @Resource
    private IOrderDiversionCommonService ordersDiversionCommonService;


    @PutMapping("division")
    public void orderDivision(@RequestBody OrderParam orderParam) {
        ordersDiversionCommonService.diversion(orderParam);
    }
}
