package com.cskaoyan.duolai.clean.orders.dispatch.client;

import com.cskaoyan.duolai.clean.housekeeping.dto.ConfigRegionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 内部接口 - 区域相关接口
 */
@FeignClient(name = "housekeeping", contextId = "housekeeping-region", path = "/housekeeping/inner/region")
public interface RegionApi {

    /**
     * 获取所有区域的业务调度配置
     *
     * @return 业务调度配置
     */
    @GetMapping("/findAllConfigRegion")
    List<ConfigRegionDTO> findAll();

    /**
     * 根据区域id获取区域调度配置
     *
     * @param id 区域id
     * @return 区域调度配置
     */
    @GetMapping("/findConfigRegionById/{id}")
    ConfigRegionDTO findConfigRegionById(@PathVariable("id") Long id);

    /**
     * 根据城市编码获取区域配置
     *
     * @param cityCode 城市编码
     * @return
     */
    @GetMapping("/findConfigRegionByCityCode")
    ConfigRegionDTO findConfigRegionByCityCode(@RequestParam("cityCode") String cityCode);


}
