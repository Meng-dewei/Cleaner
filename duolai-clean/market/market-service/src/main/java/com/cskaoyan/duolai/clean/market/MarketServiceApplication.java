package com.cskaoyan.duolai.clean.market;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = "com.cskaoyan.duolai.clean")
@Slf4j
@MapperScan("com.cskaoyan.duolai.clean.market.dao.mapper")
@EnableFeignClients
public class MarketServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(MarketServiceApplication.class)
                .build(args)
                .run(args);
        log.info("家政服务-营销中心启动");
    }
}
