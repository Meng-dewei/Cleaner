package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.common.model.PageResult;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.common.model.dto.PageRequest;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeProviderCancelDTO;
import com.cskaoyan.duolai.clean.order.dispatch.dto.OrderServeDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderServeDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeDetailDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.OrderServeStatusDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderServeInfoDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.request.OrderServePageRequest;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeFinishedCommand;
import com.cskaoyan.duolai.clean.orders.dispatch.request.ServeStartCommand;

import java.util.List;

/**
 * <p>
 * 服务服务单 服务类
 * </p>
 */
public interface IOrderServeManagerService extends IService<OrderServeDO> {
    /**
     * 列表查询
     *
     * @param currentUserId 用户id
     * @param serveStatus 服务状态
     * @param sortBy 排序字段
     * @return
     */
    List<OrderServeInfoDTO> queryForList(Long currentUserId, Integer serveStatus, Long sortBy);


    /**
     * 服务端删除订单，服务端和机构端视为不可见并非逻辑删除或实际删除
     *
     * @param id
     * @param serveProviderId
     */
    void deleteServe(Long id, Long serveProviderId, Integer serveProviderType);

    /**
     *
     * @param serveStartCommand
     * @param serveProviderId
     */
    void serveStart(ServeStartCommand serveStartCommand, Long serveProviderId);


    void serveFinished(ServeFinishedCommand serveFinishedCommand, Long serveProviderId, Integer serveProviderType);


    /**
     * 获取服务单明细
     * @param id
     * @param serveProviderId
     * @return
     */
    OrderServeDetailDTO getDetail(Long id, Long serveProviderId);

    /**
     * 取消服务,机构端、服务端前端调用，会产生违约记录，影响接派单
     * 待分配、待服务、待上门
     *
     * @param orderServeProviderCancelDTO
     * @param serveProviderId
     */
    void cancelByProvider(OrderServeProviderCancelDTO orderServeProviderCancelDTO, Long serveProviderId);

    /**
     * 取消服务单方法，已结算的服务单取消状态不变，其他结算状态变为不可结算状态
     * 服务单已完成：表示服务单状态变为退单，其他服务状态变为取消
     * 取消订单不会产生违约记录，不影响订单接单，派单行为
     *
     * @param ordersId
     */
    void cancelByUserAndOperationWithStatus(Long ordersId, Integer status);

    /**
     * 统计服务人员的状态机对应的数量
     *
     * @param serveProviderId
     * @return
     */
    OrderServeStatusDTO countServeStatusNum(Long serveProviderId);

    /**
     * 获取服务单信息
     * @param id
     * @return 服务单信息
     */
    OrderServeDTO queryById(Long id);
    /**
     * 查询服务人员服务数据
     *
     * @param ordersServePageQueryByCurrentUserReqDTO 分页条件
     * @return 分页结果
     */
    PageResult<ServeProviderServeInfoDTO> pageQueryByServeProvider(OrderServePageRequest ordersServePageQueryByCurrentUserReqDTO);


    /**
     * 查询服务单
     *
     * @param id 服务单id
     * @param serveProviderId 服务人员或机构id
     */
    OrderServeDO queryByIdAndServeProviderId(Long id, Long serveProviderId);


    /**
     * 根据id更新服务单信息
     *
     * @param id 服务单id
     * @param serveProviderId 服务人员或机构id
     * @param orderServeDO 服务单信息
     * @return
     */
    boolean updateOrderServe(Long id, Long serveProviderId, OrderServeDO orderServeDO);


    /**
     * 删除服务单
     *
     * @param id 服务单id
     * @param serveProviderId 服务人员或机构id
     */
    void cancelOrderServe(Long id, Long serveProviderId);


    PageDTO<OrderServeDTO> queryOrderServeByServeStatus(Integer status, PageRequest pageRequest);

}
