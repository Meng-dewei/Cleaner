package com.cskaoyan.duolai.clean.market.job;

import com.cskaoyan.duolai.clean.market.service.IActivityService;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class XxlJobHandler {


    @Resource
    private IActivityService activityService;

    @Resource
    private ICouponService couponService;

    /**
     * 活动状态修改，
     * 1.活动进行中状态修改
     * 2.活动已失效状态修改
     * 每分钟执行一次
     */
    @XxlJob("updateActivityStatus")
    public void updateActivitySatus(){
        log.info("定时修改活动状态...");
        try {
            activityService.updateStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 已领取优惠券自动过期任务
     * 每小时执行一次
     */
    @XxlJob("processExpireCoupon")
    public void processExpireCoupon() {
        log.info("已领取优惠券自动过期任务...");
        try {
            couponService.processExpireCoupon();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 活动预热，每小时预热
     *
     */
    @XxlJob("activityWarmUp")
    public void activityWarmUp() {
        log.info("优惠券活动定时预热...");
        try {
            activityService.warmUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
