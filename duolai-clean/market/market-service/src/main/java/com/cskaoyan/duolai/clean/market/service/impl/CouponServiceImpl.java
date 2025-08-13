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
        // 1.校验
        // 活动状态包括：1：待生效，2：进行中，3：已失效 4: 作废'
        if (status > CouponStatusEnum.INVALID.getStatus() || status < CouponStatusEnum.NO_USE.getStatus()) {
            throw new BadRequestException("请求状态不存在");
        }
        // 2.查询准备
        LambdaQueryWrapper<CouponDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 查询条件
        lambdaQueryWrapper
                .eq(CouponDO::getUserId, userId)
                // 查询小于上1页的排序字段值的记录
                .lt(ObjectUtils.isNotNull(lastId), CouponDO::getId, lastId)
                .eq(CouponDO::getStatus, status);

        // 排序
        lambdaQueryWrapper.orderByDesc(CouponDO::getId);
        // 查询条数限制
        lambdaQueryWrapper.last(" limit 10 ");
        // 3.查询数据
        List<CouponDO> coupons = baseMapper.selectList(lambdaQueryWrapper);
        //判空
        if (CollUtils.isEmpty(coupons)) {
            return new ArrayList<>();
        }
        // 数据转换
        return couponConverter.couponsToCouponInfoDTOs(coupons);
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
        // 1.校验活动开始时间或结束
        // 抢券时间
        ActivityInfoDTO activity = activityService.getActivityInfoByIdFromCache(seizeCouponCommand.getId());
        LocalDateTime now = DateUtils.now();
        if (activity == null ||
                activity.getDistributeStartTime().isAfter(now)) {
            throw new CommonException(ErrorInfo.Code.SEIZE_COUPON_FAILD, "活动未开始");
        }
        if (activity.getDistributeEndTime().isBefore(now)) {
            throw new CommonException(ErrorInfo.Code.SEIZE_COUPON_FAILD, "活动已结束");
        }

        // 2.抢券准备
        log.debug("dispatch coupon ,activityId->{},UserContext.currentUserId():{}", seizeCouponCommand.getId(), UserContext.currentUserId());
        // 抢券
        Long result = redissonLuaHandler.seizeCoupon(seizeCouponCommand.getId(), UserContext.currentUserId());
        log.debug("dispatch coupon result : {}", result);
        // 4.处理lua脚本结果
        if (result == -1) {
            throw new CommonException(ErrorInfo.Code.SEIZE_COUPON_FAILD, "限领一张");
        }
        if (result == -2) {
            throw new CommonException(ErrorInfo.Code.SEIZE_COUPON_FAILD, "已抢光!");
        }

        // 抢卷成功
        SeizeCouponResultDTO seizeCouponResultDTO = new SeizeCouponResultDTO();
        seizeCouponResultDTO.setUserId(UserContext.currentUserId());
        seizeCouponResultDTO.setActivityId(seizeCouponCommand.getId());

        // 抢卷成功后处理(异步后处理)
        // 发送抢卷成功的消息
        rocketMQClient.sendMessage(MqTopicConstant.COUPON_SEIZE_TOPIC, seizeCouponResultDTO);
    }

    @Override
    public List<AvailableCouponsDTO> getAvailable(BigDecimal totalAmount) {
        Long userId = UserContext.currentUserId();
        /*
            查询用户抢到的所有优惠券, 查询条件为:
            1. 用户id为指定userId
            2. 使用状态为CouponStatusEnum.NO_USE.getStatus()
            3. 还未到达过期时间：gt(CouponDO::getValidityTime, DateUtils.now())
            4. 达到用户满减或者折扣限额: le(CouponDO::getAmountCondition, totalAmount)
         */
        List<CouponDO> coupons = lambdaQuery()
                .eq(CouponDO::getUserId, userId)
                .eq(CouponDO::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .gt(CouponDO::getValidityTime, DateUtils.now())
                .le(CouponDO::getAmountCondition, totalAmount)
                .list();
        // 判空
        if (CollUtils.isEmpty(coupons)) {
            return new ArrayList<>();
        }

        /*
             2.组装数据计算优惠金额

               1) 计算每个优惠卷的优惠金额 CouponUtils.calDiscountAmount(coupon, totalAmount)
                  如果优惠卷金额
               2) 过滤得到优惠金额大于0且小于等于订单金额的优惠券
               3) 按照折扣金额降序排序
         */
        List<AvailableCouponsDTO> collect = coupons.stream()
                .peek(coupon -> coupon.setDiscountAmount(CouponUtils.calDiscountAmount(coupon, totalAmount)))
                //过滤优惠金额大于0且小于订单金额的优惠券
                .filter(coupon -> coupon.getDiscountAmount().compareTo(new BigDecimal(0)) > 0 && coupon.getDiscountAmount().compareTo(totalAmount) < 0)
                // 类型转换
                .map(coupon -> couponConverter.couponDoToAvailableCouponsDTO(coupon))
                //按优惠金额降序排
                .sorted(Comparator.comparing(AvailableCouponsDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
        return collect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponUseDTO use(CouponUseParam couponUseCommand) {
        //判空
        if (ObjectUtils.isNull(couponUseCommand.getOrdersId()) ||
                ObjectUtils.isNull(couponUseCommand.getTotalAmount())) {
            throw new BadRequestException("优惠券核销的订单信息为空");
        }
        //用户id
        Long userId = UserContext.currentUserId();
        //查询抢卷信息
        CouponDO coupon = baseMapper.selectById(couponUseCommand.getId());
        // 优惠券判空
        if (coupon == null ) {
            throw new BadRequestException("优惠券不存在");
        }
        if ( ObjectUtils.notEqual(coupon.getUserId(),userId)) {
            throw new BadRequestException("只允许核销自己的优惠券");
        }
        /*
             更新抢卷信息(自己补充更新条件和跟新的值!!!!):
             条件:
              1.  id为指定抢卷id
              2.  状态为未使用 CouponStatusEnum.NO_USE.getStatus()
              3.  还未到达过期时间gt(CouponDO::getValidityTime, DateUtils.now())
              4.  满减限额小于等于支付金额(CouponDO::getAmountCondition, couponUseCommand.getTotalAmount())
             更新:
              1. 订单id
              2. 状态为已使用状态CouponStatusEnum.USED.getStatus()
              3. 使用时间
         */
        boolean update = lambdaUpdate()
                .eq(CouponDO::getId, couponUseCommand.getId())
                .eq(CouponDO::getStatus, CouponStatusEnum.NO_USE.getStatus())
                .gt(CouponDO::getValidityTime, DateUtils.now())
                .le(CouponDO::getAmountCondition, couponUseCommand.getTotalAmount())
                .set(CouponDO::getOrdersId, couponUseCommand.getOrdersId())
                .set(CouponDO::getStatus, CouponStatusEnum.USED.getStatus())
                .set(CouponDO::getUseTime, DateUtils.now())
                .update();
        if (!update) {
            throw new DBException("优惠券核销失败");
        }

        //添加核销记录
        CouponWriteOffDO couponWriteOffDO = CouponWriteOffDO.builder()
                .id(IdUtils.getSnowflakeNextId())
                .couponId(couponUseCommand.getId())
                .userId(userId)
                .ordersId(couponUseCommand.getOrdersId())
                .activityId(coupon.getActivityId())
                .writeOffTime(DateUtils.now())
                .writeOffManName(coupon.getUserName())
                .writeOffManPhone(coupon.getUserPhone())
                .build();
        if(!couponWriteOffService.save(couponWriteOffDO)){
            throw new DBException("优惠券核销失败");
        }

        // 计算优惠金额
        BigDecimal discountAmount = CouponUtils.calDiscountAmount(coupon, couponUseCommand.getTotalAmount());
        CouponUseDTO couponUseDTO = new CouponUseDTO();
        couponUseDTO.setDiscountAmount(discountAmount);
        return couponUseDTO;
    }

    @Override
    @Transactional
    public void syncCouponSeizeInfo(SeizeCouponResultDTO couponSeizeInfo) {
        // 1.获取活动
        ActivityDO activityDO = activityService.getById(couponSeizeInfo.getActivityId());
        if (activityDO == null) {
            return;
        }
        // 2. 校验用户是否存在(发生服务调用)
        CommonUserDTO commonUserDTO = commonUserApi.findById(couponSeizeInfo.getUserId());
        if(commonUserDTO == null){
            return;
        }
        // 2.新增优惠券
        CouponDO coupon = new CouponDO();
        coupon.setActivityId(couponSeizeInfo.getActivityId());
        coupon.setUserId(couponSeizeInfo.getUserId());
        coupon.setUserName(commonUserDTO.getNickname());
        coupon.setUserPhone(commonUserDTO.getPhone());
        coupon.setName(activityDO.getName());
        coupon.setType(activityDO.getType());
        coupon.setDiscountAmount(activityDO.getDiscountAmount());
        coupon.setDiscountRate(activityDO.getDiscountRate());
        coupon.setAmountCondition(activityDO.getAmountCondition());
        // 计算所领取优惠卷的有效时间
        coupon.setValidityTime(DateUtils.now().plusDays(activityDO.getValidityDays()));
        // 设置状态为未使用状态
        coupon.setStatus(CouponStatusEnum.NO_USE.getStatus());

        // 保存用户抢卷信息
        couponService.save(coupon);

        //扣减库存
         activityService.deductStock(couponSeizeInfo.getActivityId());
    }

}
