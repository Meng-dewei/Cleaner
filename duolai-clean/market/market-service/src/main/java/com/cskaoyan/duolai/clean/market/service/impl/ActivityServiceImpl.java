package com.cskaoyan.duolai.clean.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.common.utils.*;
import com.cskaoyan.duolai.clean.market.constants.RedisConstants;
import com.cskaoyan.duolai.clean.market.constants.TabTypeConstants;
import com.cskaoyan.duolai.clean.market.converter.ActivityConverter;
import com.cskaoyan.duolai.clean.market.converter.CouponConverter;
import com.cskaoyan.duolai.clean.market.enums.ActivityStatusEnum;
import com.cskaoyan.duolai.clean.market.dto.ActivityDTO;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponInfoDTO;
import com.cskaoyan.duolai.clean.market.service.IActivityService;
import com.cskaoyan.duolai.clean.market.service.ICouponService;
import com.cskaoyan.duolai.clean.market.service.ICouponWriteOffService;
import com.cskaoyan.duolai.clean.market.dao.mapper.ActivityMapper;
import com.cskaoyan.duolai.clean.market.dao.entity.ActivityDO;
import com.cskaoyan.duolai.clean.market.request.ActivityPageRequest;
import com.cskaoyan.duolai.clean.market.request.ActivityCommand;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, ActivityDO> implements IActivityService {
    private static final int MILLION = 1000000;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ICouponService couponService;

    @Resource
    private ICouponWriteOffService couponWriteOffService;

    @Resource
    ActivityConverter activityConverter;

    @Resource
    CouponConverter couponConverter;

    @Override
    public PageDTO<ActivityDTO> queryForPage(ActivityPageRequest activityPageRequestDTO) {
        // 1.查询准备
        LambdaQueryWrapper<ActivityDO> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 查询条件
        lambdaQueryWrapper
                .eq(ObjectUtils.isNotNull(activityPageRequestDTO.getId()), ActivityDO::getId, activityPageRequestDTO.getId())
                .like(StringUtils.isNotEmpty(activityPageRequestDTO.getName()), ActivityDO::getName, activityPageRequestDTO.getName())
                .eq(ObjectUtils.isNotNull(activityPageRequestDTO.getType()), ActivityDO::getType, activityPageRequestDTO.getType())
                .eq(ObjectUtils.isNotNull(activityPageRequestDTO.getStatus()), ActivityDO::getStatus, activityPageRequestDTO.getStatus());

        // 排序
        lambdaQueryWrapper.orderByDesc(ActivityDO::getId);
        // 分页
        Page<ActivityDO> activityPage = new Page<>(activityPageRequestDTO.getPageNo().intValue(), activityPageRequestDTO.getPageSize().intValue());
        activityPage = baseMapper.selectPage(activityPage, lambdaQueryWrapper);

        return activityConverter.toActivityInfoResPage(activityPage.getRecords(),
                (int) activityPage.getTotal(), (int) activityPage.getPages());
    }

    @Override
    public ActivityInfoDTO queryById(Long id) {
        // 1.获取活动
        ActivityDO activityDO = baseMapper.selectById(id);
        // 判空
        if (activityDO == null) {
            return new ActivityInfoDTO();
        }
        // 2.数据转换，并返回信息
        ActivityInfoDTO activityInfoDTO = activityConverter.activityToActivityInfoDTO(activityDO);
        // 设置状态
        Integer status = getStatus(activityDO.getDistributeStartTime(), activityDO.getDistributeEndTime(), activityDO.getStatus());
        activityInfoDTO.setStatus(status);

        // 3.查询领取数量，并设置
        Integer receiveNum = couponService.countReceiveNumByActivityId(activityDO.getId());
        activityInfoDTO.setReceiveNum(NumberUtils.null2Zero(receiveNum));

        // 4.核销(使用)量
        Integer writeOffNum = couponWriteOffService.countByActivityId(id);
        activityInfoDTO.setWriteOffNum(NumberUtils.null2Zero(writeOffNum));

        return activityInfoDTO;
    }

    // 修改和添加
    @Override
    public void save(ActivityCommand activityCommand) {
        // 1.逻辑校验
        activityCommand.check();
        // 2.活动数据组装
        // 转换
        ActivityDO activityDO = activityConverter.activityCommandToActivityDO(activityCommand);
        // 状态
        if (activityCommand.getId() == null) {
            // 新增活动时设置状态为待生效
            activityDO.setStatus(ActivityStatusEnum.NO_DISTRIBUTE.getStatus());
        }

        //设置库存
        activityDO.setStockNum(activityCommand.getTotalNum());
        // 3.保存
        saveOrUpdate(activityDO);
    }


    @Override
    public void updateStatus() {
        LocalDateTime now = DateUtils.now();
        // 1.更新已经进行中的状态
        lambdaUpdate()
                .set(ActivityDO::getStatus, ActivityStatusEnum.DISTRIBUTING.getStatus())//更新活动状态为进行中
                .eq(ActivityDO::getStatus, ActivityStatusEnum.NO_DISTRIBUTE.getStatus())//检索待生效的活动
                .le(ActivityDO::getDistributeStartTime, now)//活动开始时间小于等于当前时间
                .gt(ActivityDO::getDistributeEndTime, now)//活动结束时间大于当前时间
                .update();

        // 2.更新已经结束的
        lambdaUpdate()
                .set(ActivityDO::getStatus, ActivityStatusEnum.LOSE_EFFICACY.getStatus())//更新活动状态为已失效
                .in(ActivityDO::getStatus, Arrays.asList(ActivityStatusEnum.DISTRIBUTING.getStatus(), ActivityStatusEnum.NO_DISTRIBUTE.getStatus()))//检索待生效及进行中的活动
                .lt(ActivityDO::getDistributeEndTime, now)//活动结束时间小于当前时间
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        // 1.活动作废
        boolean update = lambdaUpdate()
                .set(ActivityDO::getStatus, ActivityStatusEnum.VOIDED.getStatus())
                .eq(ActivityDO::getId, id)
                .in(ActivityDO::getStatus, Arrays.asList(ActivityStatusEnum.NO_DISTRIBUTE.getStatus(), ActivityStatusEnum.DISTRIBUTING.getStatus()))
                .update();
        if (!update) {
            return;
        }

        // 2.未使用优惠券作废(id表示优惠卷活动的id)
        couponService.revoke(id);

        // 3. 删除缓存中的优惠卷活动，以及其库存
        // 优惠卷活动的缓存为: redissonClient.getMap(RedisConstants.RedisKey.ACTIVITY_CACHE_LIST);
        // 优惠卷活动库存缓存为: RMap<String, Integer> map = redissonClient.getMap(RedisConstants.RedisKey.COUPON_RESOURCE_STOCK, StringCodec.INSTANCE);
        RMap<Long, SeizeCouponInfoDTO> activityCache = redissonClient.getMap(RedisConstants.RedisKey.ACTIVITY_CACHE_LIST);
        activityCache.remove(id);
        RMap<String, Integer> stockMap = redissonClient.getMap(RedisConstants.RedisKey.COUPON_RESOURCE_STOCK, StringCodec.INSTANCE);
        stockMap.remove(id.toString());
    }

    @Override
    public void warmUp() {
        //当前时间
        LocalDateTime now = DateUtils.now();

        //查询进行中还未到结束的优惠券活动， 1个月内待开始的优惠券活动
        List<ActivityDO> list = lambdaQuery()
                .le(ActivityDO::getDistributeStartTime, now.plusDays(30))//1个月内即将开始的
                .in(ActivityDO::getStatus, Arrays.asList(ActivityStatusEnum.NO_DISTRIBUTE.getStatus(), ActivityStatusEnum.DISTRIBUTING.getStatus()))//查询待开始和进行中的
                .orderByAsc(ActivityDO::getDistributeStartTime)
                .list();
        if (CollUtils.isEmpty(list)) {
            //防止缓存穿透
            list = new ArrayList<>();
        }
        // 2.数据转换
        List<SeizeCouponInfoDTO> seizeCouponInfoDTOS = couponConverter.activityDOsToSeizeCouponDTOs(list);
        Map<Long, SeizeCouponInfoDTO> seizeCouponInfoMap = seizeCouponInfoDTOS.stream()
                .collect(Collectors.toMap(SeizeCouponInfoDTO::getId, seizeCouponInfoDTO -> seizeCouponInfoDTO));

        /*
            3. 将待生效的活动放入到redis的key为RedisConstants.RedisKey.ACTIVITY_CACHE_LIST的hash数据结构中
             并且hash中的field为优惠卷活动id，值为SeizeCouponInfoDTO对象
         */
        RMap<Long, SeizeCouponInfoDTO> seizeCouponCacheList = redissonClient.getMap(RedisConstants.RedisKey.ACTIVITY_CACHE_LIST);
        seizeCouponCacheList.putAll(seizeCouponInfoMap);

        // 4. 将待生效的活动库存写入redis
        list.stream()
                .filter(v-> getStatus(v.getDistributeStartTime(),v.getDistributeEndTime(),v.getStatus())==1)
                .forEach(v->{
                    RMap<String, Integer> map = redissonClient.getMap(RedisConstants.RedisKey.COUPON_RESOURCE_STOCK, StringCodec.INSTANCE);
                    map.put(v.getId().toString(), v.getTotalNum());
                });
    }

    @Override
    public List<SeizeCouponInfoDTO> queryForListFromCache(Integer tabType) {
        //从redis获取活动信息
        RMap<Long, SeizeCouponInfoDTO> seizeCouponList = redissonClient.getMap(RedisConstants.RedisKey.ACTIVITY_CACHE_LIST);
        if (CollectionUtils.isEmpty(seizeCouponList)) {
            return CollUtils.emptyList();
        }
        // 获取库存
        RMap<String, String> stockMap = redissonClient.getMap(RedisConstants.RedisKey.COUPON_RESOURCE_STOCK, StringCodec.INSTANCE);

        //根据tabType确定要查询的状态
        int queryStatus = tabType == TabTypeConstants.SEIZING
                ? ActivityStatusEnum.DISTRIBUTING.getStatus() : ActivityStatusEnum.NO_DISTRIBUTE.getStatus();

        //访问redis，过滤状态为queryStatus数据(调用getStatus方法获取优惠卷活动实际状态)，并设置剩余数量(获取redis中的库存)、实际状态
        //过滤数据，并设置剩余数量、实际状态
        List<SeizeCouponInfoDTO> collect = seizeCouponList.values().stream().filter(item -> queryStatus == getStatus(item.getDistributeStartTime(), item.getDistributeEndTime(), item.getStatus()))
                .peek(item -> {
                    //剩余数量
                    String stockStr = stockMap.get(item.getId());
                    item.setRemainNum(Integer.parseInt(stockStr));
                    //状态
                    item.setStatus(queryStatus);
                }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public ActivityInfoDTO getActivityInfoByIdFromCache(Long id) {
        RMap<Long, SeizeCouponInfoDTO> activityCache = redissonClient.getMap(RedisConstants.RedisKey.ACTIVITY_CACHE_LIST);
        SeizeCouponInfoDTO seizeCouponInfo = activityCache.get(id);

        if (seizeCouponInfo == null) {
            return null;
        }

        RMap<String, String> stockMap = redissonClient.getMap(RedisConstants.RedisKey.COUPON_RESOURCE_STOCK, StringCodec.INSTANCE); // 声明为<String, String>
        String remainNumStr = stockMap.get(id.toString());
        Integer remainNum = remainNumStr != null ? Integer.parseInt(remainNumStr) : 0;

        ActivityInfoDTO activityInfoDTO = new ActivityInfoDTO();
        activityInfoDTO.setId(seizeCouponInfo.getId());
        activityInfoDTO.setName(seizeCouponInfo.getName());
        activityInfoDTO.setType(seizeCouponInfo.getType());
        activityInfoDTO.setAmountCondition(seizeCouponInfo.getAmountCondition());
        activityInfoDTO.setDiscountRate(seizeCouponInfo.getDiscountRate());
        activityInfoDTO.setDiscountAmount(seizeCouponInfo.getDiscountAmount());
        activityInfoDTO.setDistributeStartTime(seizeCouponInfo.getDistributeStartTime());
        activityInfoDTO.setDistributeEndTime(seizeCouponInfo.getDistributeEndTime());
        activityInfoDTO.setTotalNum(seizeCouponInfo.getTotalNum());

        int actualStatus = getStatus(
                seizeCouponInfo.getDistributeStartTime(),
                seizeCouponInfo.getDistributeEndTime(),
                seizeCouponInfo.getStatus()
        );
        activityInfoDTO.setStatus(actualStatus);

        return activityInfoDTO;
    }

    // 实时性问题，定时任务未及时更新
    private int getStatus(LocalDateTime distributeStartTime, LocalDateTime distributeEndTime, Integer status) {
        if (ActivityStatusEnum.NO_DISTRIBUTE.equals(status) &&
                distributeStartTime.isBefore(DateUtils.now()) &&
                distributeEndTime.isAfter(DateUtils.now())) {//待生效状态，实际活动已开始
            return ActivityStatusEnum.DISTRIBUTING.getStatus();
        } else if (ActivityStatusEnum.NO_DISTRIBUTE.equals(status) &&
                distributeEndTime.isBefore(DateUtils.now())) {//待生效状态，实际活动已结束
            return ActivityStatusEnum.LOSE_EFFICACY.getStatus();
        } else if (ActivityStatusEnum.DISTRIBUTING.equals(status) &&
                distributeEndTime.isBefore(DateUtils.now())) {//进行中状态，实际活动已结束
            return ActivityStatusEnum.LOSE_EFFICACY.getStatus();
        }
        return status;
    }

    /**
     * 扣减库存
     *
     * @param id 活动id
     *           如果扣减库存失败抛出异常
     */
    public void deductStock(Long id) {
        boolean update = lambdaUpdate()
                .setSql("stock_num = stock_num-1")
                .eq(ActivityDO::getId, id)
                .gt(ActivityDO::getStockNum, 0)
                .update();
        if (!update) {
            throw new CommonException("扣减优惠券库存失败，活动id:" + id);
        }
    }

}
