package com.cskaoyan.duolai.clean.market.controller.consumer;

import com.cskaoyan.duolai.clean.market.request.SeizeCouponCommand;
import com.cskaoyan.duolai.clean.market.dto.CouponDTO;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerCouponController")
@RequestMapping("/consumer/coupon")
@Api(tags = "用户端-优惠券相关接口")
public class CouponController {

    @Resource
    private ICouponService couponService;

    @GetMapping("/my")
    @ApiOperation("我的优惠券列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "lastId", value = "上一次查询最后一张优惠券id", required = false, dataTypeClass = Long.class),
            @ApiImplicitParam(name = "status", value = "优惠券状态，1:未使用，2:已使用，3:已过期", required = true, dataTypeClass = Long.class)
    })
    public List<CouponDTO> queryMyCouponForPage(@RequestParam(value = "lastId", required = false) Long lastId,
                                                @RequestParam(value = "status", required = true) Integer status) {
        return couponService.queryForList(lastId, UserContext.currentUserId(), status);
    }

    @ApiOperation("抢券接口")
    @PostMapping("/seize")
    public void seizeCoupon(@RequestBody SeizeCouponCommand seizeCouponCommand) {
        couponService.seizeCoupon(seizeCouponCommand);
    }

}
