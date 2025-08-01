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
import java.util.List;


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
        // 1. 判断当前请求是否需要登录

        // 1.1 获取当前请求的请求路径
        String uri = GatewayWebUtils.getUri(exchange);

        // 1.2 请求路径是否在不需要登录的白名单中
        List<String> accessPathWhiteList
                = applicationProperties.getAccessPathWhiteList();
        if (accessPathWhiteList.contains(uri)) {
            // 当前的请求无需登录，直接放行
            return chain.filter(exchange);
        }

        // 2. 从请求头中获取用户的jwt token字符串
        String tokenStr
                = GatewayWebUtils.getRequestHeader(exchange, HEADER_TOKEN);
        if (StringUtils.isEmpty(tokenStr)) {
            return GatewayWebUtils.toResponse(exchange, HttpStatus.FORBIDDEN.value(), ErrorInfo.Msg.REQUEST_FORBIDDEN);
        }

        // 3. 解析校验获取的jwt token字符串

        // 3.1 从token字符串中，获取当前用户的类型
        Integer userType = JwtTool.getUserType(tokenStr);

        //3.2 获取对应用户类型的秘钥
        String tokenSecret = applicationProperties.getTokenKey().get(userType + "");
        if (StringUtils.isEmpty(tokenSecret)) {
            // 秘钥为空，禁止访问
            return GatewayWebUtils.toResponse(exchange, HttpStatus.FORBIDDEN.value(), ErrorInfo.Msg.REQUEST_FORBIDDEN);
        }

        // 3.3 解析token字符串
        JwtTool jwtTool = new JwtTool(tokenSecret);
        CurrentUserInfo userInfo = jwtTool.parseToken(tokenStr);

        // 4. 将用户的登录信息放入到请求头中，转发给服务
        String userInfoJson = JsonUtils.toJsonStr(userInfo);

        // 为了防止非法字符
        String encodedStr = Base64Utils.encodeStr(userInfoJson);

        ServerWebExchange newExchange = GatewayWebUtils
                .setRequestHeader(exchange, HeaderConstants.USER_INFO, encodedStr);

        return chain.filter(newExchange);
    }
}
