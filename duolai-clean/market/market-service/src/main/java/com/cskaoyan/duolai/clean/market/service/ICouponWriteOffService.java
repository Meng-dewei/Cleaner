package com.cskaoyan.duolai.clean.market.service;

import com.cskaoyan.duolai.clean.market.dao.entity.CouponWriteOffDO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 优惠券核销表 服务类
 * </p>
 */
public interface ICouponWriteOffService extends IService<CouponWriteOffDO> {


    /**
     * 根据获取活动id统计核销量
     * @param activityId
     * @return
     */
    Integer countByActivityId(Long activityId);
}
