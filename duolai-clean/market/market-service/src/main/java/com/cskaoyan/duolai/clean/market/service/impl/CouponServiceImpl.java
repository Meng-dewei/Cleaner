package com.cskaoyan.duolai.clean.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.expcetions.BadRequestException;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.common.expcetions.DBException;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.common.utils.*;
import com.cskaoyan.duolai.clean.market.client.CommonUserApi;
import com.cskaoyan.duolai.clean.market.converter.CouponConverter;
import com.cskaoyan.duolai.clean.market.dto.AvailableCouponsDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponUseDTO;
import com.cskaoyan.duolai.clean.market.enums.CouponStatusEnum;
import com.cskaoyan.duolai.clean.market.config.RedissonLuaHandler;
import com.cskaoyan.duolai.clean.market.dao.mapper.CouponMapper;
import com.cskaoyan.duolai.clean.market.dao.entity.ActivityDO;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponDO;
import com.cskaoyan.duolai.clean.market.dao.entity.CouponWriteOffDO;
import com.cskaoyan.duolai.clean.market.request.CouponPageRequest;
import com.cskaoyan.duolai.clean.market.request.SeizeCouponCommand;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import com.cskaoyan.duolai.clean.market.dto.CouponDTO;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponResultDTO;
import com.cskaoyan.duolai.clean.market.request.CouponUseParam;
import com.cskaoyan.duolai.clean.market.service.IActivityService;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.cskaoyan.duolai.clean.market.service.ICouponWriteOffService;
import com.cskaoyan.duolai.clean.market.utils.CouponUtils;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import com.cskaoyan.duolai.clean.rocketmq.client.RocketMQClient;
import com.cskaoyan.duolai.clean.rocketmq.constant.MqTopicConstant;
import com.cskaoyan.duolai.clean.user.dto.CommonUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper, CouponDO> implements ICouponService {

    @Resource
    private RedissonLuaHandler redissonLuaHandler;


    @Resource
    private IActivityService activityService;

    @Resource
    private ICouponWriteOffService couponWriteOffService;

    @Resource
    private CouponConverter couponConverter;

    @Resource
    private RocketMQClient rocketMQClient;

    @Resource
    private ICouponService couponService;

    @Resource
    private CommonUserApi commonUserApi;

    @Override
    public PageDTO<CouponDTO> queryForPageOfOperation(CouponPageRequest couponPageRequestDTO) {
        // 1.数据校验
        if (ObjectUtils.isNull(couponPageRequestDTO.getActivityId())) {
            throw new BadRequestException("请指定活动");
        }
        // 2.数据查询
        // 分页 排序
        Page<CouponDO> couponQueryPage = PageUtils.parsePageQuery(couponPageRequestDTO, CouponDO.class);
        // 查询条件
        LambdaQueryWrapper<CouponDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CouponDO::getActivityId, couponPageRequestDTO.getActivityId());
        // 查询数据
        Page<CouponDO> couponPage = baseMapper.selectPage(couponQueryPage, lambdaQueryWrapper);

        // 3.数据转化，并返回
        return couponConverter.toCouponInfoResPage(couponPage.getRecords(),
                (int) couponPage.getTotal(), (int) couponPage.getPages());
    }

    @Override
    public List<CouponDTO> queryForList(Long lastId, Long userId, Integer status) {

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long activityId) {
        lambdaUpdate()
                .set(CouponDO::getStatus, CouponStatusEnum.VOIDED.getStatus())
                .eq(CouponDO::getActivityId, activityId)
                .eq(CouponDO::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .update();
    }

    @Override
    public Integer countReceiveNumByActivityId(Long activityId) {
        return lambdaQuery().eq(CouponDO::getActivityId, activityId)
                .count().intValue();
    }

    @Override
    public void processExpireCoupon() {
        lambdaUpdate()
                .set(CouponDO::getStatus, CouponStatusEnum.INVALID.getStatus())
                .eq(CouponDO::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .le(CouponDO::getValidityTime, DateUtils.now())
                .update();
    }

    @Override
    public void seizeCoupon(SeizeCouponCommand seizeCouponCommand) {
    }

    @Override
    public List<AvailableCouponsDTO> getAvailable(BigDecimal totalAmount) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponUseDTO use(CouponUseParam couponUseCommand) {

        return null;
    }

    @Override
    @Transactional
    public void syncCouponSeizeInfo(SeizeCouponResultDTO couponSeizeInfo) {

    }

}
