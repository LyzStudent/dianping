package com.dianping.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dianping.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;


//token刷新拦截器
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token
        String token=request.getHeader("authorization");

        //2.如果token为空，直接放行，交给loginInterceptor
        if(StrUtil.isBlank(token)){
            return true;
        }

        //3.基于token获取redis中的用户数据
        String key=RedisContants.LOGIN_USER_KEY+token;
        Map<Object,Object> userMap=stringRedisTemplate.opsForHash().entries(key);

        //4.判断用户是否存在，不存在也放行，交给loginInterceptor
        if(userMap.isEmpty()){
            return true;
        }

        //5.将查询到底Hash数据转化为UserDTO对象
        UserDTO userDTO= BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        //6.将用户信息保存到ThreadLocal
        UserHolder.saveUser(userDTO);

        //7.刷新tokenTTL
        stringRedisTemplate.expire(key,RedisContants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        //移除threadlocal用户，避免内存泄露
        UserHolder.removeUser();
    }
}
