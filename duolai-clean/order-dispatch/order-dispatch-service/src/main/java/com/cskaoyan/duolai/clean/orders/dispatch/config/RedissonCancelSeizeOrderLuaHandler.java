package com.cskaoyan.duolai.clean.orders.dispatch.config;

import com.cskaoyan.duolai.clean.orders.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@Slf4j
public class RedissonCancelSeizeOrderLuaHandler {

    @Resource
    RedissonClient redissonClient;

    @Value("${order.cancel.lua}")
    String orderSeizeCancelLuaPath;

    private String sha1;

    @PostConstruct
    public void loadScript() throws IOException {
        // 缓存脚本
        ClassPathResource classPathResource
                = new ClassPathResource(orderSeizeCancelLuaPath);
        byte[] bytes = classPathResource.getInputStream().readAllBytes();
        String luaStr = new String(bytes, StandardCharsets.UTF_8);
        sha1 = redissonClient.getScript().scriptLoad(luaStr);
        log.info("load script {}", sha1);
    }

    public Long cancelOrderSeize(Long orderId, String cityCode) {
        // 资源库存redisKey
        String resourceStockRedisKey = String.format(RedisConstants.RedisKey.ORDERS_RESOURCE_STOCK, cityCode);

        // 执行脚本，扣减商品skuId对应的库存，扣减的值为count
        return redissonClient.getScript(StringCodec.INSTANCE).evalSha(RScript.Mode.READ_WRITE,
                sha1,
                RScript.ReturnType.INTEGER,
                Arrays.asList(resourceStockRedisKey), // 脚本不涉及键，传递空列表
                orderId.toString());
    }

}
