package com.dianping.trade.config;


import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelFlowConfig {

    @PostConstruct
    public void initFlowRules(){
        List<FlowRule> rules=new ArrayList<>();

        FlowRule seckill=new FlowRule();
        //对应@SentinelResource的value
        seckill.setResource("seckill");
        //按QPS
        seckill.setGrade(RuleConstant.FLOW_GRADE_QPS);
        //阈值,可挪到配置里
        seckill.setCount(10);
        //快速失败
        seckill.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
        rules.add(seckill);

        FlowRuleManager.loadRules(rules);
    }
}
