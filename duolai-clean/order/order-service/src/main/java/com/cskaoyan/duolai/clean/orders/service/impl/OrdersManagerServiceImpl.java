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
import com.cskaoyan.duolai.clean.common.utils.ObjectUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
        LambdaQueryWrapper<OrdersDO> queryWrapper = Wrappers.<OrdersDO>lambdaQuery()
                .eq(ObjectUtil.isNotEmpty(orderPageRequestDTO.getContactsPhone()), OrdersDO::getContactsPhone, orderPageRequestDTO.getContactsPhone())
                .between(ObjectUtil.isAllNotEmpty(orderPageRequestDTO.getMinCreateTime(), orderPageRequestDTO.getMaxCreateTime()), OrdersDO::getCreateTime, orderPageRequestDTO.getMinCreateTime(), orderPageRequestDTO.getMaxCreateTime())
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
        LambdaQueryWrapper<OrdersDO> queryWrapper = Wrappers.<OrdersDO>lambdaQuery()
                .in(OrdersDO::getId, orderPageRequestDTO.getOrdersIdList())
                .eq(ObjectUtils.isNotNull(orderPageRequestDTO.getUserId()), OrdersDO::getUserId, orderPageRequestDTO.getUserId());

        //2.查询
        page.setSearchCount(false);
        Page<OrdersDO> ordersPage = baseMapper.selectPage(page, queryWrapper);
        if (ObjectUtil.isEmpty(ordersPage.getRecords())) {
            return Collections.emptyList();
        }

        return ordersPage.getRecords();
    }


    @Override
    public void orderSeizeSuccess(Long id) {

    }

    @Override
    public void orderServeStart(Long id) {

    }


    @Override
    public void orderServeFinish(Long id, LocalDateTime localDateTime) {

    }

    /**
     * 根据订单id查询
     *
     * @param id 订单id
     * @return 订单详情
     */
    @Override
    public OrderInfoDTO getDetail(Long id) {

        return null;
    }



    /**
     * 取消订单
     *
     * @param orderCancelDTO 取消订单模型
     */
    @Override
    public void cancel(OrderCancelDTO orderCancelDTO) {

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
        //1.(需要你们自己写)构建查询条件：指定用户的，指定订单状态的订单，且sortBy小于参数sortBy(注意使用condition参数的重载方法)
        LambdaQueryWrapper<OrdersDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrdersDO::getUserId, currentUserId)
                .eq(ObjectUtils.isNotEmpty(ordersStatus), OrdersDO::getOrdersStatus, ordersStatus)
                .lt(ObjectUtils.isNotEmpty(sortBy),OrdersDO::getSortBy, sortBy);
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
