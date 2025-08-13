package com.cskaoyan.duolai.clean.market.config;

import com.cskaoyan.duolai.clean.market.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RedissonLuaHandler {

    @Resource
    RedissonClient redissonClient;

    @Value("${market.seize.coupon.lua}")
    String seizeCouponLuaPath;

    private String sha1;

    // 自定义初始化方法
    @PostConstruct
    public void loadScript() throws IOException {
        // 缓存脚本
        ClassPathResource classPathResource
                = new ClassPathResource(seizeCouponLuaPath);
        byte[] bytes = classPathResource.getInputStream().readAllBytes();
        // lua脚本字符串
        String luaStr = new String(bytes, StandardCharsets.UTF_8);
        // 上传lua脚本，redis会缓存这个脚本，返回一个标识值
        sha1 = redissonClient.getScript().scriptLoad(luaStr);
        log.info("load script {}", sha1);
    }

    public Long seizeCoupon(Long activityId, Long userId) {
        // 资源库存redisKey
        String resourceStockRedisKey = RedisConstants.RedisKey.COUPON_RESOURCE_STOCK;
        // 抢券列表
        String couponSeizeListRedisKey = String.format(RedisConstants.RedisKey.COUPON_SEIZE_LIST, userId);

        // 执行脚本，
        return redissonClient.getScript(StringCodec.INSTANCE).evalSha(RScript.Mode.READ_WRITE,
                sha1,
                RScript.ReturnType.INTEGER,
                Arrays.asList(resourceStockRedisKey, couponSeizeListRedisKey), // 脚本不涉及键，传递空列表  key值集合
                activityId.toString()); // 参数值结合
    }

}
