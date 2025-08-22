package com.cskaoyan.duolai.clean.orders.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.DbRuntimeException;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.common.expcetions.CommonException;
import com.cskaoyan.duolai.clean.common.model.dto.PageDTO;
import com.cskaoyan.duolai.clean.common.utils.JsonUtils;
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
import com.cskaoyan.duolai.clean.market.request.CouponUseBackParam;
import com.cskaoyan.duolai.clean.mvc.utils.UserContext;
import com.cskaoyan.duolai.clean.orders.client.*;
import com.cskaoyan.duolai.clean.orders.constants.FieldConstants;
import com.cskaoyan.duolai.clean.orders.converter.OrderConverter;
import com.cskaoyan.duolai.clean.orders.enums.*;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersCanceledDO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersDO;
import com.cskaoyan.duolai.clean.orders.model.entity.OrdersRefundDO;
import com.cskaoyan.duolai.clean.orders.dto.OrderCancelDTO;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSimpleDTO;
import com.cskaoyan.duolai.clean.orders.dto.OrderUpdateDTO;
import com.cskaoyan.duolai.clean.orders.request.OrderPageRequest;
import com.cskaoyan.duolai.clean.orders.service.*;
//import com.jzo2o.api.market.CouponApi;
import com.cskaoyan.duolai.clean.orders.dto.OrderInfoDTO;
import com.cskaoyan.duolai.clean.common.constants.UserType;
import com.cskaoyan.duolai.clean.mysql.utils.PageUtils;
import com.cskaoyan.duolai.clean.orders.config.OrderStateMachine;
import com.cskaoyan.duolai.clean.orders.model.mapper.OrdersMapper;
import com.cskaoyan.duolai.clean.orders.model.dto.OrderSnapshotDTO;
import com.cskaoyan.duolai.clean.orders.strategy.OrderCancelStrategyManager;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 */
@Slf4j
@Service
public class OrdersManagerServiceImpl extends ServiceImpl<OrdersMapper, OrdersDO> implements IOrdersManagerService {

    @Resource
    private IOrdersCreateService ordersCreateService;


    @Resource
    private ServeProviderApi serveProviderApi;

    @Resource
    private OrderStateMachine orderStateMachine;

    @Resource
    IOrdersCanceledService ordersCanceledService;

    @Resource
    IOrdersCommonService ordersCommonService;

    @Resource
    private IOrdersRefundService ordersRefundService;

    @Autowired
    OrdersMapper ordersMapper;

    @Resource
    OrderConverter orderConverter;


    @Autowired
    OrdersManagerServiceImpl owner;

    @Autowired
    CouponApi couponApi;

    @Autowired
    OrderCancelStrategyManager orderCancelStrategyManager;

    /**
     * 管理端 - 分页查询订单列表
     *
     * @param
     */


    @Override
    public List<OrdersDO> batchQuery(List<Long> ids) {
        LambdaQueryWrapper<OrdersDO> queryWrapper = Wrappers.<OrdersDO>lambdaQuery().in(OrdersDO::getId, ids).ge(OrdersDO::getUserId, 0);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public OrdersDO queryById(Long id) {
        return baseMapper.selectById(id);
    }


    /**
     * 管理端 - 分页查询订单id列表
     *
     * @param orderPageRequestDTO 分页查询模型
     * @return 分页结果
     */
    @Override
    public Page<Long> operationPageQueryOrdersIdList(OrderPageRequest orderPageRequestDTO) {
        //1.构造查询条件
        Page<OrdersDO> page = PageUtils.parsePageQuery(orderPageRequestDTO, OrdersDO.class);

        // 查询条件，电话不空查询电话，时间不空查询时间，但是只查询id
        LambdaQueryWrapper<OrdersDO> queryWrapper = null;
        queryWrapper
                .eq(ObjectUtils.isNotEmpty( orderPageRequestDTO.getContactsPhone()), OrdersDO::getContactsPhone, orderPageRequestDTO.getContactsPhone())
                .between(ObjectUtils.isAllNotEmpty(orderPageRequestDTO.getMinCreateTime(), orderPageRequestDTO.getMinCreateTime())
                        ,OrdersDO::getCreateTime, orderPageRequestDTO.getMinCreateTime(), orderPageRequestDTO.getMinCreateTime())
                .select(OrdersDO::getId);


        //2.分页查询
        Page<OrdersDO> ordersPage = baseMapper.selectPage(page, queryWrapper);

        //3.封装结果，查询数据为空，直接返回
        Page<Long> orderIdsPage = new Page<>();
        if (ObjectUtil.isEmpty(ordersPage.getRecords())) {
            return orderIdsPage;
        }

        //4.查询结果不为空，提取订单id封装
        List<Long> orderIdList = ordersPage.getRecords().stream()
                .map(OrdersDO::getId).collect(Collectors.toList());
        orderIdsPage.setTotal(ordersPage.getTotal());
        orderIdsPage.setPages(ordersPage.getPages());
        orderIdsPage.setRecords(orderIdList);
        return orderIdsPage;
    }

    /**
     * 根据订单id列表查询并排序
     *
     * @param orderPageRequestDTO 订单分页查询请求
     * @return 订单列表
     */
    @Override
    public List<OrdersDO> queryAndSortOrdersListByIds(OrderPageRequest orderPageRequestDTO) {
        //1.构造查询条件
        Page<OrdersDO> page = new Page<>();
        page.setSize(orderPageRequestDTO.getPageSize());
        // 放入排序字段
        page.setOrders(PageUtils.getOrderItems(orderPageRequestDTO, OrdersDO.class));
        // 查询条件，指定用户的订单，且订单id在orderPageRequestDTO.getOrdersIdList()集合中
        LambdaQueryWrapper<OrdersDO> queryWrapper = null;
        queryWrapper
                .in(OrdersDO::getId, orderPageRequestDTO.getOrdersIdList());

        //2.查询
        page.setSearchCount(false);
        List<OrdersDO> ordersPage = baseMapper.selectList(queryWrapper);
        if (ObjectUtil.isEmpty(ordersPage)) {
            return Collections.emptyList();
        }

        return ordersPage;
    }


    @Override
    @Transactional
    public void orderSeizeSuccess(Long id) {
//        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder().id(id)
//                .originStatus(OrderStatusEnum.DISPATCHING.getStatus())
//                .targetStatus(OrderStatusEnum.NO_SERVE.getStatus())
//                .build();
//
//        // 更新订单状态
//        int result = ordersCommonService.updateStatus(orderUpdateDTO);
//        if (result <= 0) {
//            throw new DbRuntimeException("待服务订单关闭事件处理失败");
//        }
        // 抢单成功只需要修改订单状态即可，不需要在订单快照中保存其他额外信息
        orderStateMachine.changeStatus(String.valueOf(id), OrderStatusChangeEventEnum.DISPATCH, null);
    }

    @Override
    public void orderServeStart(Long id) {
        // 服务开始时只需要修改订单状态即可，不需要在订单快照中保存其他额外信息
        orderStateMachine.changeStatus(String.valueOf(id), OrderStatusChangeEventEnum.START_SERVE, null);
    }


    @Override
    public void orderServeFinish(Long id, LocalDateTime localDateTime) {
        // 服务完成时，也需要记录一下服务完成时间，所以构造了一个新的快照订单
        OrderSnapshotDTO snapshotDTO = OrderSnapshotDTO.builder().realServeEndTime(localDateTime).build();
        orderStateMachine.changeStatus(String.valueOf(id), OrderStatusChangeEventEnum.COMPLETE_SERVE, snapshotDTO);
    }

    /**
     * 根据订单id查询
     *
     * @param id 订单id
     * @return 订单详情
     */
    @Override
    public OrderInfoDTO getDetail(Long id) {
        // 获取缓存中的订单最新的快照数据
        String currentSnapshotJson = orderStateMachine.getCurrentSnapshotCache(id.toString());
        OrderSnapshotDTO orderSnapshotDTO = JsonUtils.toBean(currentSnapshotJson, OrderSnapshotDTO.class);

        //如果支付过期则取消订单
        if (ObjectUtils.equals(orderSnapshotDTO.getPayStatus(), OrderStatusEnum.NO_PAY.getStatus())) {
            orderSnapshotDTO = cancelIfPayOvertime(orderSnapshotDTO);
        }

        return orderConverter.orderSnapshotDTOtoOrderDTO(orderSnapshotDTO);
    }

    private OrderSnapshotDTO cancelIfPayOvertime(OrderSnapshotDTO orderSnapshotDTO) {
        //创建订单未支付15分钟后自动取消
        if (orderSnapshotDTO.getCreateTime().plusMinutes(15).isBefore(LocalDateTime.now())
                && orderSnapshotDTO.getPayStatus() == OrderPayStatusEnum.NO_PAY.getStatus()) {
            //查询支付结果，如果支付最新状态仍是未支付进行取消订单
            int payResultFromTradServer = ordersCreateService.getPayResult(orderSnapshotDTO.getId());
            if (payResultFromTradServer != OrderPayStatusEnum.PAY_SUCCESS.getStatus()) {
                //取消订单
                OrderCancelDTO orderCancelDTO = orderConverter.orderSnapshotDTOtoCancelDTO(orderSnapshotDTO);
                orderCancelDTO.setCurrentUserType(UserType.SYSTEM);
                orderCancelDTO.setCancelReason("订单超时支付，自动取消");
                owner.cancel(orderCancelDTO);
            }

            //从快照中查询订单数据
            String jsonResult = orderStateMachine.getCurrentSnapshotCache(String.valueOf(orderSnapshotDTO.getId()));
            orderSnapshotDTO = JSONUtil.toBean(jsonResult, OrderSnapshotDTO.class);
            return orderSnapshotDTO;
        }
        return orderSnapshotDTO;

    }


    /**
     * 取消订单
     *
     * @param orderCancelDTO 取消订单模型
     */
    @Override
    @GlobalTransactional
    public void cancel(OrderCancelDTO orderCancelDTO) {
        OrdersDO ordersDO = getById(orderCancelDTO.getId());
        orderCancelDTO.setCityCode(ordersDO.getCityCode());
        orderCancelDTO.setRealPayAmount(ordersDO.getRealPayAmount());
        orderCancelDTO.setTradingOrderNo(ordersDO.getTradingOrderNo());
//        orderCancelDTO.setUserId(ordersDO.getUserId());

        orderCancelDTO.setServeStartTime(ordersDO.getServeStartTime());

        //订单状态
        Integer ordersStatus = ordersDO.getOrdersStatus();

        BigDecimal discountAmount = ordersDO.getDiscountAmount();

//        if (discountAmount.doubleValue() > 0) {
//            // 使用了优惠卷，那么需要退回优惠卷
//            CouponUseBackParam couponUseBackCommand = new CouponUseBackParam();
//            couponUseBackCommand.setUserId(orderCancelDTO.getUserId());
//            couponUseBackCommand.setOrdersId(orderCancelDTO.getId());
//            // 调用营销服务
//            couponApi.useBack(couponUseBackCommand);
//        }
//
//        if (OrderStatusEnum.NO_PAY.getStatus().equals(ordersStatus)) { //订单状态为待支付
//            owner.cancelByNoPay(orderCancelDTO);
//            return;
//        }
//
//        if (OrderStatusEnum.DISPATCHING.getStatus().equals(ordersStatus)) { //订单状态为待服务
//            owner.cancelByDispatching(orderCancelDTO);
//            return;
//        }
//
        orderCancelStrategyManager.cancel(orderCancelDTO, ordersStatus, discountAmount);

        throw new CommonException("当前订单状态不支持取消");
    }


    @Transactional(rollbackFor = Exception.class)
    public void cancelByNoPay(OrderCancelDTO orderCancelDTO) {
        OrdersCanceledDO ordersCanceledDO = orderConverter.orderCancelDTOtoCanceledDO(orderCancelDTO);
        ordersCanceledDO.setCancellerId(orderCancelDTO.getCurrentUserId());
        ordersCanceledDO.setCancelerName(orderCancelDTO.getCurrentUserName());
        ordersCanceledDO.setCancellerType(orderCancelDTO.getCurrentUserType());
        ordersCanceledDO.setCancelTime(LocalDateTime.now());

        // 保存订单取消记录
        ordersCanceledService.save(ordersCanceledDO);

//        //更新订单状态为取消订单
//        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder()
//                .id(orderCancelDTO.getId())
//                .originStatus(OrderStatusEnum.NO_PAY.getStatus())
//                .targetStatus(OrderStatusEnum.CANCELED.getStatus())
//                .build();
//
//        // 更新订单状态
//        int result = ordersCommonService.updateStatus(orderUpdateDTO);
//        if (result <= 0) {
//            throw new DbRuntimeException("订单取消事件处理失败");
//        }

        //构建订单快照更新模型
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //订单状态变更
        orderStateMachine.changeStatus( orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CANCEL, orderSnapshotDTO);


    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelByDispatching(OrderCancelDTO orderCancelDTO) {
        //保存取消订单记录
        OrdersCanceledDO ordersCanceledDO = orderConverter.orderCancelDTOtoCanceledDO(orderCancelDTO);
        ordersCanceledDO.setCancellerId(orderCancelDTO.getCurrentUserId());
        ordersCanceledDO.setCancelerName(orderCancelDTO.getCurrentUserName());
        ordersCanceledDO.setCancellerType(orderCancelDTO.getCurrentUserType());
        ordersCanceledDO.setCancelTime(LocalDateTime.now());

        // 保存取消订单记录
        ordersCanceledService.save(ordersCanceledDO);

        //更新订单状态为关闭订单
        OrderUpdateDTO orderUpdateDTO = OrderUpdateDTO.builder().id(orderCancelDTO.getId())
                .originStatus(OrderStatusEnum.DISPATCHING.getStatus())
                .targetStatus(OrderStatusEnum.CLOSED.getStatus())
                .refundStatus(OrderRefundStatusEnum.REFUNDING.getStatus())//退款状态为退款中
                .build();

        // 更新订单状态
        int result = ordersCommonService.updateStatus(orderUpdateDTO);
        if (result <= 0) {
            throw new DbRuntimeException("待服务订单关闭事件处理失败");
        }

        // 保存取消订单记录
        OrderSnapshotDTO orderSnapshotDTO = OrderSnapshotDTO.builder()
                .cancellerId(orderCancelDTO.getCurrentUserId())
                .cancelerName(orderCancelDTO.getCurrentUserName())
                .cancellerType(orderCancelDTO.getCurrentUserType())
                .cancelReason(orderCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();

        //订单状态变更
        orderStateMachine.changeStatus( orderCancelDTO.getId().toString(), OrderStatusChangeEventEnum.CLOSE_DISPATCHING_ORDER, orderSnapshotDTO);

        //添加退款记录
        OrdersRefundDO ordersRefundDO = new OrdersRefundDO();
        ordersRefundDO.setId(orderCancelDTO.getId());
        ordersRefundDO.setTradingOrderNo(orderCancelDTO.getTradingOrderNo());
        ordersRefundDO.setRealPayAmount(orderCancelDTO.getRealPayAmount());
        // 保存退款记录(
        ordersRefundService.save(ordersRefundDO);
    }



    /**
     * 管理端 - 分页查询
     *
     * @param orderPageRequestDTO 分页查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<OrderSimpleDTO> operationPageQuery(OrderPageRequest orderPageRequestDTO) {
        //1.分页查询订单id列表
        Page<Long> ordersIdPage = operationPageQueryOrdersIdList(orderPageRequestDTO);
        if (ObjectUtil.isEmpty(ordersIdPage.getRecords())) {
            return null;
        }

        //2.根据订单id列表查询订单
        orderPageRequestDTO.setOrdersIdList(ordersIdPage.getRecords());
        List<OrdersDO> ordersDOList = queryAndSortOrdersListByIds(orderPageRequestDTO);

        //3.封装响应结果
        return orderConverter.toOrderSimplePage(ordersDOList, (int) ordersIdPage.getTotal(), (int) ordersIdPage.getPages());
    }

    /**
     * 滚动分页查询
     *
     * @param currentUserId 当前用户id
     * @param ordersStatus  订单状态，0：待支付，100：派单中，200：待服务，300：服务中，500：订单完成，600：已取消，700：已关闭
     * @param sortBy        排序字段
     * @return 订单列表
     */
    @Override
    public List<OrderSimpleDTO> consumerQueryList(Long currentUserId, Integer ordersStatus, Long sortBy) {
        //1.构建查询条件：指定用户的，指定订单状态的订单，且sortBy小于参数sortBy(注意使用condition参数的重载方法)
        LambdaQueryWrapper<OrdersDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.
                eq(OrdersDO::getUserId, currentUserId)
                .lt(ObjectUtils.isNotEmpty(sortBy), OrdersDO::getSortBy, sortBy)
                .eq(ObjectUtils.isNotEmpty(ordersStatus),OrdersDO::getOrdersStatus, ordersStatus);

        Page<OrdersDO> queryPage = new Page<>();
        queryPage.addOrder(OrderItem.desc(FieldConstants.SORT_BY));
        // 只需要查前10条即可
        queryPage.setTotal(10);
        // 不查询满足条件的总条数
        queryPage.setSearchCount(false);

        //2.查询订单id列表
        Page<OrdersDO> ordersPage = baseMapper.selectPage(queryPage, queryWrapper);
        if (ObjectUtil.isEmpty(ordersPage.getRecords())) {
            return new ArrayList<>();
        }

        return orderConverter.orderDOsToOrderSimpleDTOs(ordersPage.getRecords());
    }
}
