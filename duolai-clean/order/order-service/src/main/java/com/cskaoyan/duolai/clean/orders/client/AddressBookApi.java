package com.cskaoyan.duolai.clean.orders.client;

import com.cskaoyan.duolai.clean.user.dto.AddressBookDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user", contextId = "user-address", path = "/user/inner/address-book")
public interface AddressBookApi {
    @GetMapping("/getByUserIdAndCity")
   List<AddressBookDTO> getByUserIdAndCity(@RequestParam("userId") Long userId, @RequestParam("city") String city);
    @GetMapping("/{id}")
    AddressBookDTO detail(@PathVariable("id") Long id);
}
