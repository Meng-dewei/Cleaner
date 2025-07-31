package com.cskaoyan.duolai.clean.gateway.filter;


import cn.hutool.core.util.IdUtil;
import com.cskaoyan.duolai.clean.gateway.properties.ApplicationProperties;
import com.cskaoyan.duolai.clean.gateway.utils.GatewayWebUtils;
import com.cskaoyan.duolai.clean.common.constants.ErrorInfo;
import com.cskaoyan.duolai.clean.common.constants.HeaderConstants;
import com.cskaoyan.duolai.clean.common.model.CurrentUserInfo;
import com.cskaoyan.duolai.clean.common.utils.Base64Utils;
import com.cskaoyan.duolai.clean.common.utils.JsonUtils;
import com.cskaoyan.duolai.clean.common.utils.JwtTool;
import com.cskaoyan.duolai.clean.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;


/**
 * token解析过滤器
 */
@Slf4j
@Component
public class TokenFilter implements GlobalFilter {

    /**
     * token header名称
     */
    private static final String HEADER_TOKEN = "Authorization";

    // 注入关于登录白名单
    @Resource
    private ApplicationProperties applicationProperties;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 4.请求放行
        return chain.filter(exchange);
    }
}
