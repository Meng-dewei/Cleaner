package com.cskaoyan.duolai.clean.market.controller.operation;


import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.request.CouponPageRequest;
import com.cskaoyan.duolai.clean.market.dto.CouponDTO;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController("operationCouponController")
@RequestMapping("/operation/coupon")
@Api(tags = "运营端-优惠券相关接口")
public class CouponController {

    @Resource
    private ICouponService couponService;

    @GetMapping("/page")
    @ApiOperation("根据活动id查询领取记录")
    public PageDTO<CouponDTO> queryForPage(CouponPageRequest couponPageRequest) {
        return couponService.queryForPageOfOperation(couponPageRequest);
    }
}
