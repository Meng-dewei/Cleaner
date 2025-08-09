package com.cskaoyan.duolai.clean.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cskaoyan.duolai.clean.common.expcetions.BadRequestException;
import com.cskaoyan.duolai.clean.common.expcetions.DBException;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.market.enums.CouponStatusEnum;
import com.cskaoyan.duolai.clean.market.dao.mapper.CouponWriteOffMapper;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponDO;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponWriteOffDO;
import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.cskaoyan.duolai.clean.market.service.ICouponUseBackService;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponUseBackDO;
import com.cskaoyan.duolai.clean.market.dao.mapper.CouponUseBackMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券使用回退记录 服务实现类
 * </p>
 */
@Service
public class CouponUseBackServiceImpl extends ServiceImpl<CouponUseBackMapper, CouponUseBackDO> implements ICouponUseBackService {

    @Resource
    private ICouponService couponService;

    @Resource
    CouponWriteOffMapper couponWriteOffMapper;

    @Resource
    RedissonClient redissonClient;

    @Override
    @Transactional
    public void useBack(CouponUseBackParam couponUseBackCommand) {
        Long userId = couponUseBackCommand.getUserId();
        Long ordersId = couponUseBackCommand.getOrdersId();


        // 根据用户id和订单id查询优惠卷核销信息
        LambdaQueryWrapper<CouponWriteOffDO> usedOrderCouponWrapper = Wrappers.lambdaQuery(CouponWriteOffDO.class)
                .eq(CouponWriteOffDO::getUserId, userId)
                .eq(CouponWriteOffDO::getOrdersId, ordersId);

        CouponWriteOffDO couponWriteOffDO = couponWriteOffMapper.selectOne(usedOrderCouponWrapper);

        Long couponId = couponWriteOffDO.getCouponId();
        // 查询用户以抢优惠卷信息
        CouponDO coupon = couponService.getById(couponId);
        if (ObjectUtils.isEmpty(coupon) || ObjectUtils.notEqual(coupon.getUserId(), userId)) {
            throw new BadRequestException("已抢优惠卷不存在");
        }

        LocalDateTime nowTime = LocalDateTime.now();
        // 计算用户已抢优惠卷的状态(有可能优惠卷已过期)
        CouponStatusEnum status = coupon.getValidityTime().isAfter(nowTime) ? CouponStatusEnum.NO_USE : CouponStatusEnum.INVALID;

        /*
            更新用户已强优惠卷状态，将userTime置为null，将ordersId置为null
         */

        /*
             保存优惠卷回退信息
             1. couponId
             2. userId
             3. use_back时间
             4. 优惠卷核销时间 coupon表的user_time
         */
    }
}
