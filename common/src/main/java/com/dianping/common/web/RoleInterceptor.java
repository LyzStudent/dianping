package com.dianping.common.web;

import com.dianping.common.annotation.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod hm)){
            return true;
        }
        RequireRole rr=hm.getMethodAnnotation(RequireRole.class);
        if(rr==null){
            return true;//没标注释，放行
        }
        String role=request.getHeader("X-User_Role");//网关注入
        for(String r:rr.value()){
            if(r.equals(role)){
                return true;
            }
        }
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"errorMsg\":\"无权访问\"}");
        return false;
    }
}
