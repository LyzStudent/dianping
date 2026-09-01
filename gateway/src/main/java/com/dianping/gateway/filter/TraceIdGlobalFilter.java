package com.dianping.gateway.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 生成/透传 traceId,保证一个请求在多个服务间日志可串查
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER="X-Trace-Id";
    public static final String TRACE_ID_KEY="traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId=exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if(!StringUtils.hasText(traceId)){
            traceId= UUID.randomUUID().toString().replace("-","");
        }
        MDC.put(TRACE_ID_KEY,traceId);
        ServerHttpRequest newRequest=exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER,traceId)
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build())
                .doFinally(signal->MDC.remove(TRACE_ID_KEY));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
