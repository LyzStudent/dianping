package com.dianping.common.feign;

import com.dianping.common.dto.UserDTO;
import com.dianping.common.util.UserHolder;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign调用时把当前登录用户上下文转发给下游
 * @ConditionalOnClass:user/trade服务没有引入OpenFeign,扫描到此类时自动跳过，避免ClassNotFound
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
public class FeignConfig {

    @Bean
    public RequestInterceptor userContextFeignInterceptor(){
        return template ->{
            //traceId透传:为空传空字符串，避免 Feign 写入 null header 报错；下游对空值会重新生成
            String traceId = MDC.get("traceId");
            template.header("X-Trace-Id", StringUtils.hasText(traceId) ? traceId : "");
            UserDTO user= UserHolder.getUser();
            if(user!=null){
                template.header("X-User_Id",String.valueOf(user.getId()));
                template.header("X-User_NickName",user.getNickName());
                template.header("X-User_Icon",user.getIcon());
                //角色必须透传：下游 @RequireRole + RoleInterceptor 依赖该 header 做鉴权
                template.header("X-User_Role", user.getRole()==null ? "1" : String.valueOf(user.getRole()));
            }
        };
    }
}
