package com.cskaoyan.duolai.clean.orders.dispatch.controller.worker;

import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderSeizeCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderSerizeRequest;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderSeizePageDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeListDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderServeManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Api(tags = "服务端 - 抢单相关接口")
@RequestMapping("/worker")
@Slf4j
public class WorkerOrderSeizeController {

    @Resource
    private IOrderSeizeService ordersSeizeService;

    @Resource
    private IOrderServeManagerService ordersServeManagerService;

    @GetMapping("/queryForList")
    @ApiOperation("服务单列表提供给服务端")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serveStatus", value = "服务状态，0：待分配，1：待服务，2：服务中，3：服务完成，4：已取消，5：被退单", required = true, dataTypeClass = Long.class),
            @ApiImplicitParam(name = "sortBy", value = "排序字段", required = true, dataTypeClass = Long.class)
    })
    public OrderServeListDTO queryForList(@RequestParam(value = "serveStatus",required = false) Integer serveStatus, @RequestParam(value = "sortBy",required = false) Long sortBy) {
        List<OrderServeInfoDTO> orderServeInfoDTOS = ordersServeManagerService.queryForList(UserContext.currentUserId(), serveStatus, sortBy);
        return new OrderServeListDTO(orderServeInfoDTOS);
    }

    @GetMapping("")
    @ApiOperation("服务端抢单列表")
    public List<OrderSeizePageDTO> queryForList(OrderSerizeRequest orderSerizeRequest) {
        return ordersSeizeService.queryForList(orderSerizeRequest);
    }

    @PostMapping("")
    @ApiOperation("服务端抢单")
    public void seize(@RequestBody OrderSeizeCommand orderSeizeCommand) {
        ordersSeizeService.seize(orderSeizeCommand.getId(), UserContext.currentUserId(), UserContext.currentUser().getUserType(), false);
    }
}
