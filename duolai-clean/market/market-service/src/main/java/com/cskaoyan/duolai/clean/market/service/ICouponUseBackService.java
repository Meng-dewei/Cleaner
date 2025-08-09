package com.cskaoyan.duolai.clean.market.service;

import com.cskaoyan.duolai.clean.market.dao.entity.CouponUseBackDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;

/**
 * <p>
 * 优惠券使用回退记录 服务类
 * </p>
 */
public interface ICouponUseBackService extends IService<CouponUseBackDO> {
    /**
     * 用户退回优惠卷
     */
    void useBack(CouponUseBackParam couponUseBackCommand);
}
