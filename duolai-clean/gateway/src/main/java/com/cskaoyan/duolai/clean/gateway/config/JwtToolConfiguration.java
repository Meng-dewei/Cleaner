package com.cskaoyan.duolai.clean.gateway.config;

import cn.hutool.extra.spring.SpringUtil;
import com.cskaoyan.duolai.clean.gateway.constants.UserConstants;
import com.cskaoyan.duolai.clean.gateway.properties.ApplicationProperties;
import com.cskaoyan.duolai.clean.common.utils.JwtTool;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

/**
 * 生成多个token解析器
 */
@Configuration
public class JwtToolConfiguration {

    @Resource
    private ApplicationProperties applicationProperties;

    /**
     * 初始化JwtTool
     */
    @PostConstruct
    public void initJwtTools() {
        for (Map.Entry<String, String> entry : applicationProperties.getTokenKey().entrySet()) {
            String beanName = UserConstants.JWT_TOKEN_BEAN_NAME + entry.getKey();
            JwtTool jwtTool = new JwtTool(entry.getValue());
            SpringUtil.registerBean(beanName, jwtTool);
        }
    }

}
