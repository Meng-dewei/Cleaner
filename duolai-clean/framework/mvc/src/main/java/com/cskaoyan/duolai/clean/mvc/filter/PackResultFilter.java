package com.cskaoyan.duolai.clean.mvc.filter;

import com.cskaoyan.duolai.clean.common.model.Result;
import com.cskaoyan.duolai.clean.common.utils.IoUtils;
import com.cskaoyan.duolai.clean.mvc.wrapper.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.cskaoyan.duolai.clean.mvc.constants.HeaderConstants.BODY_PROCESSED;

/**
 * 用于包装外网访问结果
 */
@Component
@Slf4j
public class PackResultFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1.无需包装，放过拦截
        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
        if (requestURI.contains(".") ||
                requestURI.contains("/swagger") ||
                requestURI.contains("/api-docs") ||
                requestURI.contains("/inner")) {
            // 注意这里对服务调用时直接放行的
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // 2.包装响应值
        // 2.1.处理业务，获取响应值
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        ResponseWrapper responseWrapper = new ResponseWrapper(response);
        filterChain.doFilter(servletRequest, responseWrapper);

        // 无需包装
        if (response.containsHeader(BODY_PROCESSED) && response.getHeader(BODY_PROCESSED).equals("1")) {
            IoUtils.write(response.getOutputStream(), false, responseWrapper.getResponseData());
            return;
        }

        // 2.2.包装
        // responseWrapper.getResponseData()获取响应体数据
        //  {"code":200, "message":"ok", data: dataresponseWrapper.getResponseData() + "}"
        byte[] bytes = Result.plainOk(responseWrapper.getResponseData());
        log.info("result : {}", new String(bytes));
        // 2.3.写入
        response.setContentType("applicaton/json;charset=UTF-8");
        response.setContentLength(bytes.length);
        IoUtils.write(response.getOutputStream(), false, bytes);
    }
}
