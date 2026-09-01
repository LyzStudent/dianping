package com.dianping.common.web;

import com.dianping.common.config.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 每个服务都注册两个拦截器（网关只扫common.jwt包，不会扫这里避免servlet泄露给WebFlux）
 */
@Configuration
public class CommoWebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SecurityProperties securityProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor()).order(-1);
        registry.addInterceptor(new UserContextInterceptor()).order(0);
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(securityProperties.getExcludepaths())
                .order(1);
        registry.addInterceptor(new RoleInterceptor()).order(2);
    }
}
