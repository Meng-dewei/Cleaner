package com.cskaoyan.duolai.clean.market.client;

import com.cskaoyan.duolai.clean.user.dto.CommonUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 内部接口 - 普通用户相关接口
 */
@FeignClient(name = "user", contextId = "user-common-user", path = "/user/inner/common-user")
public interface CommonUserApi {

    @GetMapping("{id}")
    CommonUserDTO findById(@PathVariable("id") Long id);
}
