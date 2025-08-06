package com.cskaoyan.duolai.clean.orders;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.cskaoyan.duolai.clean"})
@EnableAspectJAutoProxy
@Slf4j
@MapperScan("com.cskaoyan.duolai.clean.**.mapper")
@EnableTransactionManagement
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(OrderServiceApplication.class)
                .build(args)
                .run(args);
        log.info("家政服务-订单管理微服务启动");
    }
}
