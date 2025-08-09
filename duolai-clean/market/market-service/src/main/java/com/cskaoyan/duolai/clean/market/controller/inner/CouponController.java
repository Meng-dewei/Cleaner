package com.cskaoyan.duolai.clean.market.controller.inner;


import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponUseDTO;
import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;
import com.cskaoyan.duolai.clean.market.request.CouponUseParam;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.cskaoyan.duolai.clean.market.service.ICouponUseBackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@RestController("innerCouponController")
@RequestMapping("/inner/coupon")
@Api(tags = "内部接口-优惠券相关接口")
public class CouponController {


    @Resource
    private ICouponService couponService;

    @Resource
    private ICouponUseBackService couponUseBackService;


    @GetMapping("/getAvailable")
    @ApiOperation("获取可用优惠券列表")
    @ApiImplicitParam(name = "totalAmount", value = "总金额", required = true, dataTypeClass = BigDecimal.class)
    public List<AvailableCouponsDTO> getAvailable(@RequestParam("totalAmount") BigDecimal totalAmount) {
        return couponService.getAvailable(totalAmount);
    }


    @PostMapping("/use")
    @ApiOperation("使用优惠券，并返回优惠金额")
    public CouponUseDTO use(@RequestBody CouponUseParam couponUseCommand) {
        CouponUseDTO use = couponService.use(couponUseCommand);
        return use;
    }


    @PostMapping("/useBack")
    @ApiOperation("优惠券退回接口")
    public void useBack(@RequestBody CouponUseBackParam couponUseBackCommand) {
        couponUseBackService.useBack(couponUseBackCommand);
    }
}
