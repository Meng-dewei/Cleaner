package com.cskaoyan.duolai.clean.market.service;

import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.market.dao.entity.ActivityDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.market.request.ActivityPageRequest;
import com.cskaoyan.duolai.clean.market.request.ActivityCommand;
import com.cskaoyan.duolai.clean.market.dto.ActivityDTO;
import com.cskaoyan.duolai.clean.market.dto.ActivityInfoDTO;
import com.cskaoyan.duolai.clean.market.dto.SeizeCouponInfoDTO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IActivityService extends IService<ActivityDO> {

    /**
     * 分页查询活动
     *
     * @param activityPageRequestDTO
     * @return
     */
    PageDTO<ActivityDTO> queryForPage(ActivityPageRequest activityPageRequestDTO);

    ActivityInfoDTO queryById(Long id);

    /**
     * 活动保存
     *
     * @param activityCommand
     */
    void save(ActivityCommand activityCommand);



    /**
     * 更新活动状态，
     * 1.已经进行中但是状态未修改的订单变为进行中
     * 2.
     */
    void updateStatus();

    /**
     * 活动作废
     * 1.活动变为作废
     * 2.活动中产生的未使用的优惠券作废
     */
    void revoke(Long id);


    /**
     * 活动预热
     */
    void warmUp();

    /**
     * 用户端抢券列表分页查询活动信息
     *
     * @param tabType 页面类型
     * @return
     */
    List<SeizeCouponInfoDTO> queryForListFromCache(Integer tabType);

    /**
     * 从缓存中获取活动信息
     * @param id
     * @return
     */
    ActivityInfoDTO getActivityInfoByIdFromCache(Long id);

    /**
     * 扣减库存
     * @param id 活动id
     *  如果扣减库存失败抛出异常
     */
    void deductStock(Long id);
}
