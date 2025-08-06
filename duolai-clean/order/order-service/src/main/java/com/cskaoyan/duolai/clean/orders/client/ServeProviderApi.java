package com.cskaoyan.duolai.clean.orders.client;

import com.cskaoyan.duolai.clean.user.dto.ServeProviderInfoDTO;
import com.cskaoyan.duolai.clean.user.dto.ServeProviderSimpleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 内部接口 - 服务人员/机构相关接口
 */
@FeignClient(name = "user", contextId = "user-serv-provider", path = "/user/inner/serve-provider", qualifiers = "customerServeProviderApi")
public interface ServeProviderApi {

    @GetMapping("/{id}")
    ServeProviderInfoDTO getDetail(@PathVariable("id") Long id);

    /**
     * 批量获取服务人员/机构所在城市编码
     * @param ids
     * @return
     */
    @GetMapping("/batchCityCode")
    Map<Long, String> batchCityCode(@RequestParam("ids") List<Long> ids);

//    /**
//     * 根据服务人员id或机构id获取服务人员或机构的类型
//     * @param ids 服务人员或机构id列表
//     * @return 服务人员或机构id和对应的服务人员或机构类型
//     */
//    @GetMapping("/batchGetProviderType")
//    Map<Long, Integer> batchGetProviderType(@RequestParam("ids") List<Long> ids);
//
//    /**
//     * 批量获取服务人员/机构信息
//     * @param ids id列表
//     * @return 批量获取服务
//     */
//    @GetMapping("/batchGet")
//    List<ServeProviderSimpleDTO> batchGet(@RequestParam("ids") List<Long> ids);
}
