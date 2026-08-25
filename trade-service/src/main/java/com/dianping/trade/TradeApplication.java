package com.dianping.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.dianping")
@MapperScan("com.dianping.trade.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.dianping.common.feign")
public class TradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }
}
