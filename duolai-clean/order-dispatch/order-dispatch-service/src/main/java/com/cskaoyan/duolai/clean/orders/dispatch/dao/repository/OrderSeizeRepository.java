package com.cskaoyan.duolai.clean.orders.dispatch.dao.repository;

import com.cskaoyan.duolai.clean.common.model.OrdersSeizeInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OrderSeizeRepository extends ElasticsearchRepository<OrdersSeizeInfo, Long> {

}
