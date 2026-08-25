package com.dianping.common.web;

import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.dianping.common.dto.UserDTO;
import com.dianping.common.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截器1：把网关注入的X-User-* 请求头写入 UserHolder
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId=request.getHeader("X-User_Id");
        if(StringUtils.hasText(userId)) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(Long.valueOf(userId));
            userDTO.setNickName(request.getHeader("X-User_NickName"));
            userDTO.setIcon(request.getHeader("X-User_Icon"));
            UserHolder.saveUser(userDTO);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
