package com.cskaoyan.duolai.clean.orders.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.common.model.PageResult;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSimpleDTO;
import com.cskaoyan.duolai.clean.orders.request.OrderPageRequest;
import com.cskaoyan.duolai.clean.orders.dto.OperationOrdersDetailDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderInfoDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 订单表 服务类
 * </p>
 */
public interface IOrdersManagerService extends IService<OrdersDO> {

    /**
     * @param ids
     * @return
     */
    List<OrdersDO> batchQuery(List<Long> ids);

    // 服务调用接口使用
    OrdersDO queryById(Long id);

    /**
     * 根据订单id查询
     *
     * @param id 订单id
     * @return 订单详情
     */
    OrderInfoDTO getDetail(Long id);


    /**
     * 取消订单
     *
     * @param orderCancelDTO 取消订单模型
     */
    void cancel(OrderCancelDTO orderCancelDTO);



    /**
     * 管理端 - 分页查询
     *
     * @param orderPageRequestDTO 分页查询条件
     * @return 分页结果
     */
    PageDTO<OrderSimpleDTO> operationPageQuery(OrderPageRequest orderPageRequestDTO);

    /**
     * 滚动分页查询
     *
     * @param currentUserId 当前用户id
     * @param ordersStatus  订单状态，0：待支付，100：派单中，200：待服务，300：服务中 500：订单完成，600：已取消，700：已关闭
     * @param sortBy        排序字段
     * @return 订单列表
     */
    List<OrderSimpleDTO> consumerQueryList(Long currentUserId, Integer ordersStatus, Long sortBy);



    /**
     * 管理端 - 分页查询订单id列表
     *
     * @param orderPageRequestDTO 分页查询模型
     * @return 分页结果
     */
    Page<Long> operationPageQueryOrdersIdList(OrderPageRequest orderPageRequestDTO);

    /**
     * 根据订单id列表查询并排序
     *
     * @param orderPageRequestDTO 订单分页查询请求
     * @return 订单列表
     */
    List<OrdersDO> queryAndSortOrdersListByIds(OrderPageRequest orderPageRequestDTO);



    /**
     * 订单对应的服务开始
     *
     * @param id 订单id
     */
    void orderSeizeSuccess(Long id);


    void orderServeStart(Long id);

    void orderServeFinish(Long id, LocalDateTime localDateTime);

}
