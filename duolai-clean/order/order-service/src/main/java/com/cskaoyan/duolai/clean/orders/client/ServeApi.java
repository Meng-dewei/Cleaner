package com.cskaoyan.duolai.clean.orders.client;

import com.cskaoyan.duolai.clean.housekeeping.dto.ServeDetailDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "housekeeping", contextId = "housekeeping-serve", path = "/housekeeping/inner/serve")
public interface ServeApi {

    @GetMapping("/{id}")
    ServeDetailDTO findById(@PathVariable("id") Long id);

}
