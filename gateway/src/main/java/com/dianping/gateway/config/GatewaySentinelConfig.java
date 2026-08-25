package com.dianping.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initGatewayRules(){
        Set<GatewayFlowRule> rules=new HashSet<>();
        //按路由id限流
        rules.add(new GatewayFlowRule("trade-service").setCount(100).setIntervalSec(1));
        rules.add(new GatewayFlowRule("user-service").setCount(100).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
    }
}
