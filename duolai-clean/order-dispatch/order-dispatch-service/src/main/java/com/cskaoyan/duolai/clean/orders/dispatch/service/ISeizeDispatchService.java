package com.cskaoyan.duolai.clean.orders.dispatch.service;

public interface ISeizeDispatchService {


    /**
     * 清理资源池
     * @param cityCode 城市编码
     * @param id 资源id
     */
    void clearSeizeDispatchPool(String cityCode,Long id);

}
