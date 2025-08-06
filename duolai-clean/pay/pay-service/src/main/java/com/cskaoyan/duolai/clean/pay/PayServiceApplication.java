package com.cskaoyan.duolai.clean.pay;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@MapperScan("com.cskaoyan.duolai.clean.pay.mapper")
@ComponentScan(basePackages = {"com.cskaoyan.duolai.clean"})
@SpringBootApplication
@EnableFeignClients
public class PayServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(PayServiceApplication.class)
                .build(args)
                .run(args);
        log.info("家政服务-支付服务启动");
    }
}
