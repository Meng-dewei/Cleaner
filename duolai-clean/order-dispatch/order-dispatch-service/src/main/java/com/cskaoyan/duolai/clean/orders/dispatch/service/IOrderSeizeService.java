package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderSeizeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderSerizeRequest;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderSeizePageDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 抢单池 服务类
 * </p>
 */
@Service
public interface IOrderSeizeService extends IService<OrderSeizeDO> {


    /**
     * 当前时间距离服务预约时间间隔小于配置值时进入派单
     *
     * @param cityCode        城市编码
     * @param timeoutInterval 抢单成功
     * @return
     */
    List<OrderSeizeDO> queryNeedToDispatchSeizeOrders(String cityCode, Integer timeoutInterval);

    /**
     * 批量将订单改为抢单超时
     *
     * @param ids 抢单id
     */
    void batchTimeout(List<Long> ids);


    /**
     * 查询到达预约时间还未抢单成功的记录
     * @return
     */
    List<OrderSeizeDO> queryArriveServeStartTimeSeizeOrder();


    /**
     * 查询当前用户可以服务的抢单记录
     *
     * @param orderSerizeRequest 抢单分页查询条件
     *
     * @return 抢单数据
     */
    List<OrderSeizePageDTO> queryForList(OrderSerizeRequest orderSerizeRequest);


    /**
     * 抢单
     * @param id 抢单id
     * @param serveProviderId 服务人员或机构id
     * @param serveProviderType 用户类型，2：服务人员，3：机构
     * @param isMatchine 是否是机器抢单
     */
    void seize(Long id, Long serveProviderId, Integer serveProviderType, Boolean isMatchine);


    /**
     * 抢单成功处理结果
     */
    void seizeOrdersSuccess(Long seizeOrderId, Long serveProviderId, boolean isMachineSeize);

}
