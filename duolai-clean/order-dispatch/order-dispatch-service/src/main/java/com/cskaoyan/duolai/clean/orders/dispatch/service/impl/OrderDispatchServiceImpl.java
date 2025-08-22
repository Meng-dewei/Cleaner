package com.cskaoyan.duolai.clean.orders.dispatch.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cskaoyan.duolai.clean.housekeeping.dto.ConfigRegionDTO;
import com.cskaoyan.duolai.clean.order.dispatch.param.OrderSeizeParam;
import com.cskaoyan.duolai.clean.common.utils.*;
import com.cskaoyan.duolai.clean.orders.constants.FieldConstants;
import com.cskaoyan.duolai.clean.orders.dispatch.client.RegionApi;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.DispatchStrategyEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper.OrdersDispatchMapper;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.OrderDispatchDO;
import com.cskaoyan.duolai.clean.orders.dispatch.dto.ServeProviderDispatchDTO;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderDispatchService;
import com.cskaoyan.duolai.clean.orders.dispatch.service.IOrderSeizeService;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChain;
import com.cskaoyan.duolai.clean.orders.dispatch.strategys.IDispatchChainManager;
import com.cskaoyan.duolai.clean.orders.utils.ServeTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.cskaoyan.duolai.clean.common.model.ServeProviderInfo;


/**
 * <p>
 * 派单池 服务实现类
 * </p>
 */
@Service
@Slf4j
public class OrderDispatchServiceImpl extends ServiceImpl<OrdersDispatchMapper, OrderDispatchDO> implements IOrderDispatchService {

    @Resource
    private RegionApi regionApi;


    @Resource
    private IDispatchChainManager dispatchStrategyManager;


    @Resource
    private IOrderSeizeService ordersSeizeService;


    public void dispatch(OrderDispatchDO ordersDispatch) {

        // 1.3.服务时间,格式yyyyMMddHHmm
        Long serveTime = ServeTimeUtils.getServeTimeLong(ordersDispatch.getServeStartTime());
        // 1.4.区域调度配置
        ConfigRegionDTO configRegionDTO = regionApi.findConfigRegionByCityCode(ordersDispatch.getCityCode());
        // 1.5.获取派单规则
        DispatchStrategyEnum dispatchStrategyEnum = DispatchStrategyEnum.of(configRegionDTO.getDispatchStrategy());

        // 2.获取派单人员
        List<ServeProviderDispatchDTO> serveProvidersOfServe = searchDispatchInfo(ordersDispatch.getCityCode(),
                ordersDispatch.getServeItemId(),
                configRegionDTO.getStaffServeRadius(),
                serveTime,
                dispatchStrategyEnum,
                ordersDispatch.getLon(),
                ordersDispatch.getLat(),
                configRegionDTO.getStaffReceiveOrderMax());
        log.info("派单筛选前数据,id:{},{}",ordersDispatch.getId(), serveProvidersOfServe);
        if (CollUtils.isEmpty(serveProvidersOfServe)) {
            log.info("id:{}匹配不到人", ordersDispatch.getId());
            return;
        }

        ServeProviderDispatchDTO serveProvider = serveProvidersOfServe.get(0);

        // 4.机器抢单
        OrderSeizeParam orderSeizeParam = new OrderSeizeParam();
        // ordersDispatch.getId() 这个id表示抢单id(订单id)
        orderSeizeParam.setSeizeId(ordersDispatch.getId());
        orderSeizeParam.setServeProviderId(serveProvider.getId());
        // 服务从业者
        orderSeizeParam.setServeProviderType(2);

        ordersSeizeService.seize(orderSeizeParam.getSeizeId(), orderSeizeParam.getServeProviderId(), orderSeizeParam.getServeProviderType(), true);

    }

    private static final String[] INCLUDE_FIELD_NAMES = new String[]{
            LambdaUtils.getFieldName(ServeProviderInfo::getId),
            LambdaUtils.getUnderLineFieldName(ServeProviderInfo::getAcceptanceNum)
    };

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public List<ServeProviderDispatchDTO> searchDispatchInfo(String cityCode, long serveItemId, double maxDistance, Long serveTime, DispatchStrategyEnum dispatchStrategyEnum, Double lon, Double lat, int limit) {

        NativeSearchQueryBuilder nativeQueryBuilder = new NativeSearchQueryBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 城市条件
        TermQueryBuilder cityTermQuery = QueryBuilders.termQuery(LambdaUtils
                .getUnderLineFieldName(ServeProviderInfo::getCityCode), cityCode);
        boolQueryBuilder.filter(cityTermQuery);

        // 服务提供者是否开启接单
        TermQueryBuilder pickUpTermQuery = QueryBuilders.termQuery(LambdaUtils
               .getUnderLineFieldName(ServeProviderInfo::getPickUp), 1);
        boolQueryBuilder.filter(pickUpTermQuery);

        // 服务提供者是否完成了全部设置
        TermQueryBuilder settingStatusTermQuery = QueryBuilders.termQuery(LambdaUtils
              .getUnderLineFieldName(ServeProviderInfo::getSettingStatus), 1);
        boolQueryBuilder.filter(settingStatusTermQuery);

        // 服务提供者是否有服务时间冲突
        TermQueryBuilder serveTimesTermQuery = QueryBuilders.termQuery(LambdaUtils
                .getUnderLineFieldName(ServeProviderInfo::getServeTimes), serveTime);
        boolQueryBuilder.mustNot(serveTimesTermQuery);


        // 是否已经超出接单数量
        RangeQueryBuilder acceptanceNumTermQuery = QueryBuilders.rangeQuery(LambdaUtils
                .getUnderLineFieldName(ServeProviderInfo::getAcceptanceNum)).gte(limit);
        boolQueryBuilder.mustNot(acceptanceNumTermQuery);

        // 服务项目是否匹配
        TermQueryBuilder serveItemIdsTermQuery = QueryBuilders.termQuery(LambdaUtils
                .getUnderLineFieldName(ServeProviderInfo::getServeItemIds), serveItemId);
        boolQueryBuilder.filter(serveItemIdsTermQuery);

        // 距离条件
        GeoDistanceQueryBuilder distanceQuery = QueryBuilders.geoDistanceQuery(FieldConstants.LOCATION)
                .point(lat, lon)
                .distance(maxDistance, DistanceUnit.KILOMETERS);
        boolQueryBuilder.filter(distanceQuery);

        // 设置复合查询
        nativeQueryBuilder.withQuery(boolQueryBuilder);

        List<SortBuilder<?>> sortBuilder = getSortBuilder(dispatchStrategyEnum, lon, lat);

        // 设置排序
        nativeQueryBuilder.withSorts(sortBuilder);

        // 设置返回字段
        nativeQueryBuilder.withSourceFilter(new FetchSourceFilter(INCLUDE_FIELD_NAMES, null));
        // 设置查询数量
        nativeQueryBuilder.withPageable(PageRequest.of(0, 1));


        SearchHits<ServeProviderInfo> searchHit = elasticsearchRestTemplate.search(nativeQueryBuilder.build(), com.cskaoyan.duolai.clean.common.model.ServeProviderInfo.class);

        if (searchHit.isEmpty()) {
            // 没找到
            return null;
        }

        List<ServeProviderDispatchDTO> result = searchHit.stream().map(hit -> {
            // 获取查询到的文档对象
            ServeProviderInfo content = hit.getContent();

            ServeProviderDispatchDTO serveProviderDispatchDTO = new ServeProviderDispatchDTO();
            serveProviderDispatchDTO.setId(content.getId());
            // 如果还没接过单，则为0
            Integer acceptanceNum = content.getAcceptanceNum() == null ? 0 : content.getAcceptanceNum();
            serveProviderDispatchDTO.setAcceptanceNum(acceptanceNum);
            // 针对距离优先策略设置距离
            if (DispatchStrategyEnum.DISTANCE.equals(dispatchStrategyEnum)) {
                String distanceStr = hit.getSortValues().get(0).toString();
                BigDecimal distanceDecimal = new BigDecimal(distanceStr);
                serveProviderDispatchDTO.setAcceptanceDistance(distanceDecimal.intValue());
            }
            return serveProviderDispatchDTO;
        }).collect(Collectors.toList());

        return result;
    }


    /**
     * 排序策略
     *
     * @param dispatchStrategyEnum
     * @return
     */
    private List<SortBuilder<?>> getSortBuilder(DispatchStrategyEnum dispatchStrategyEnum, Double lon, Double lat) {
        List<SortBuilder<?>> sortBuilders = new ArrayList<>();
        switch (dispatchStrategyEnum) {
            case DISTANCE:
                GeoDistanceSortBuilder geoDistanceSortBuilder = SortBuilders
                        .geoDistanceSort(FieldConstants.LOCATION, lat, lon)
                        .unit(org.elasticsearch.common.unit.DistanceUnit.KILOMETERS)
                        .geoDistance(GeoDistance.ARC)
                        .order(SortOrder.ASC);
                sortBuilders.add(geoDistanceSortBuilder);
                break;
            case LEAST_ACCEPT_ORDER:
                FieldSortBuilder leastAcceptOrder = SortBuilders.fieldSort(LambdaUtils.getUnderLineFieldName(ServeProviderInfo::getAcceptanceNum))
                        .order(SortOrder.ASC)
                        //不存在接单量按0处理
                        .missing(0);
                sortBuilders.add(leastAcceptOrder);

        }
        return sortBuilders;
    }
}
