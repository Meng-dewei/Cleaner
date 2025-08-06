package com.cskaoyan.duolai.clean.rocketmq.config;

import com.cskaoyan.duolai.clean.rocketmq.properties.RocketMqProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class RocketMqConfiguration {

    @Resource
    RocketMqProperties mqProperties;
}
