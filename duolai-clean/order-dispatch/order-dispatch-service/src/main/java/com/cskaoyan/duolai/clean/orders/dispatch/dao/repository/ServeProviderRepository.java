package com.cskaoyan.duolai.clean.orders.dispatch.dao.repository;

import com.cskaoyan.duolai.clean.common.model.ServeProviderInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ServeProviderRepository extends ElasticsearchRepository<ServeProviderInfo, Long> {
}
