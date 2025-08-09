package com.cskaoyan.duolai.clean.market.service;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponUseDTO;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.market.request.CouponPageRequest;
import com.cskaoyan.duolai.clean.market.request.SeizeCouponCommand;
import com.cskaoyan.duolai.clean.market.dto.CouponDTO;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponResultDTO;
import com.cskaoyan.duolai.clean.market.request.CouponUseParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface ICouponService extends IService<CouponDO> {

    /**
     * 运营端查询同一个活动的优惠券
     * @param couponPageRequestDTO
     * @return
     */
    PageDTO<CouponDTO> queryForPageOfOperation(CouponPageRequest couponPageRequestDTO);

    /**
     * 滚动查询用户优惠券列表
     *
     * @param lastId 上一批查询最后一条优惠券的id
     * @param userId 查询用户的id
     * @param status 优惠券状态
     * @return 优惠券列表
     */
    List<CouponDTO> queryForList(Long lastId, Long userId, Integer status);



    /**
     * 作废指定活动未使用的优惠券
     * @param activityId
     */
    void revoke(Long activityId);

    /**
     * 统计获取优惠券领取数量
     *
     * @param activityId
     * @return
     */
    Integer countReceiveNumByActivityId(Long activityId);

    /**
     * 过期优惠券处理
     */
    void processExpireCoupon();

    /**
     * 抢券
     *
     * @param seizeCouponCommand
     */
    void seizeCoupon(SeizeCouponCommand seizeCouponCommand);


    /**
     * 获取可用优惠券列表
     * @param totalAmount
     * @return
     */
    List<AvailableCouponsDTO> getAvailable(BigDecimal totalAmount);

    /**
     * 使用优惠券
     * @param couponUseCommand
     */
    CouponUseDTO use(CouponUseParam couponUseCommand);


    void syncCouponSeizeInfo(SeizeCouponResultDTO couponResultDTO);


}
