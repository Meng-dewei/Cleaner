package com.cskaoyan.duolai.clean.orders.client;

//import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
//import com.cskaoyan.duolai.clean.market.dto.CouponUseDTO;
//import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;
//import com.cskaoyan.duolai.clean.market.request.CouponUseParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
//
@FeignClient(name = "market", contextId = "market-coupon", path = "/market/inner/coupon")
public interface CouponApi {

//    /**
//     * 获取可用优惠券列表，并按照优惠金额从大到小排序
//     *
//     * @param totalAmount 总金额，单位分
//     */
//    @GetMapping("/getAvailable")
//    List<AvailableCouponsDTO> getAvailable(@RequestParam("totalAmount") BigDecimal totalAmount);
//
//    @PostMapping("/use")
//    CouponUseDTO use(@RequestBody CouponUseParam couponUseCommand);
//
//    /**
//     * 优惠券退款回退
//     */
//    @PostMapping("/useBack")
//    void useBack(@RequestBody CouponUseBackParam couponUseBackCommand);
}
