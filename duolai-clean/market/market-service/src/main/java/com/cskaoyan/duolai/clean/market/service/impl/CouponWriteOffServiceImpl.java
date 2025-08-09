package com.cskaoyan.duolai.clean.market.service.impl;

import com.cskaoyan.duolai.clean.market.service.ICouponWriteOffService;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponWriteOffDO;
import com.cskaoyan.duolai.clean.market.dao.mapper.CouponWriteOffMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 优惠券核销表 服务实现类
 * </p>
 */
@Service
public class CouponWriteOffServiceImpl extends ServiceImpl<CouponWriteOffMapper, CouponWriteOffDO> implements ICouponWriteOffService {


    @Override
    public Integer countByActivityId(Long activityId) {
        return lambdaQuery().eq(CouponWriteOffDO::getActivityId, activityId)
                .count().intValue();
    }

}
