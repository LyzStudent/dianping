package com.dianping.gateway.filter;

import com.dianping.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.dianping.common.util.RedisContants.LOGIN_BLACKLIST_KEY;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ReactiveStringRedisTemplate stringRedisTemplate;

    @Value("${hmdp.security.excludepaths}")
    private List<String> excludepaths;

    //受保护后台路径：即使命中 /shop/** 白名单通配，也必须先校验登录（防止 /shop/admin/** 等被放行漏掉角色头）
    @Value("${hmdp.security.protectpaths:}")
    private List<String> protectpaths;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        //0.受保护后台路径优先判断：命中则必须登录并注入角色头
        //  （否则 /shop/admin/** 会被白名单 /shop/** 放行，下游 @RequireRole 读不到角色返回403）
        for (String pattern : protectpaths) {
            if (PATH_MATCHER.match(pattern, path)) {
                return authAndForward(exchange, chain);
            }
        }

        //1.白名单直接放行
        for (String pattern : excludepaths) {
            if (PATH_MATCHER.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        //2.其余路径一律要求登录
        return authAndForward(exchange, chain);
    }

    /**
     * 校验token（未登录/过期/拉黑）后注入 X-User_* 请求头，再转发下游服务
     */
    private Mono<Void> authAndForward(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        //取token
        String token = request.getHeaders().getFirst("authorization");
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "未登录");
        }

        //解析JWT，过期/篡改直接401
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (Exception e) {
            return unauthorized(exchange, "登录已过期，请重新登录");
        }

        //登出黑名单校验（响应式Redis，不阻塞Netty事件循环线程）
        return stringRedisTemplate.hasKey(LOGIN_BLACKLIST_KEY + token)
                .flatMap(banned -> {
                    if (Boolean.TRUE.equals(banned)) {
                        return unauthorized(exchange, "登录已失效");
                    }
                    //通过：把用户信息注入请求头，转发下游服务
                    Object nickName = claims.get("nickName");
                    Object icon = claims.get("icon");
                    Object role=claims.get("role");
                    ServerHttpRequest newRequest = request.mutate()
                            .header("X-User_Id", claims.getSubject())
                            .header("X-User_NickName", nickName == null ? "" : nickName.toString())
                            .header("X-User_Icon", icon == null ? "" : icon.toString())
                            .header("X-User_Role",role==null?"1":role.toString())
                            .build();
                    return chain.filter(exchange.mutate().request(newRequest).build());
                });
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = ("{\"success\":false,\"errorMsg\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
