package com.dianping.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 从请求头取traceId塞进MDC，让服务日志带上traceId
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId=request.getHeader(TRACE_ID_HEADER);
        if(!StringUtils.hasText(traceId)){
            traceId= UUID.randomUUID().toString().replace("-","");
        }
        MDC.put(TRACE_ID_KEY,traceId);
        response.setHeader(TRACE_ID_HEADER,traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        MDC.remove(TRACE_ID_KEY);
    }
}
