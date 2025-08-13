package com.cskaoyan.duolai.clean.orders.dispatch.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cskaoyan.duolai.clean.orders.dispatch.enums.BreachTypeEnum;
import com.cskaoyan.duolai.clean.orders.dispatch.dao.entity.BreachRecordDO;

/**
 * <p>
 * 违约记录 服务类
 * </p>
 */
public interface IBreachRecordService extends IService<BreachRecordDO> {


    /**
     * 单独添加违约记录
     *
     * @param breachRecordDO 违约信息
     */
    void add(BreachRecordDO breachRecordDO);


    /**
     * 统计违约记录
     * @param serveProviderId 服务人员或机构id
     * @param breachTypeEnum 拒单类型
     * @return
     */
    int count(Long serveProviderId, BreachTypeEnum breachTypeEnum, int breachDay);

}
