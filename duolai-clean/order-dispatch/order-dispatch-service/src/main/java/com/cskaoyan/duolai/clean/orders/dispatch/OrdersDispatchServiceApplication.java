package com.cskaoyan.duolai.clean.orders.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication(scanBasePackages = "com.cskaoyan.duolai.clean")
@Slf4j
@EnableFeignClients
@MapperScan({"com.cskaoyan.duolai.clean.orders.dispatch.dao.mapper"})
public class OrdersDispatchServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrdersDispatchServiceApplication.class)
                .build(args)
                .run(args);
        log.info("家政服务-抢单微服务启动");
    }
}
