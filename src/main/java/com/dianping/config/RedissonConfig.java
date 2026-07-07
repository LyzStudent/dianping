package com.dianping.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ObjectInputFilter;

//Redisson 客户端配置类
@Configuration
public class RedissonConfig {


    @Bean
    public RedissonClient redissonClient(){
        Config config=new Config();
        config.useSingleServer()//当前使用的是单节点的redis
                .setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }
}
