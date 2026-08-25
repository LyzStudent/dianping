package com.dianping.shop.config;

import com.dianping.shop.service.ShopSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShopIndexInitializer implements ApplicationRunner {

    @Resource
    private ShopSearchService shopSearchService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try{
            //内含ensureIndex，且ES没起也不会拖垮启动
            shopSearchService.syncAll();
        }catch (Exception e){
            log.warn("ES 初始化失败(确认ES 已启动并安装IK插件): {}",e.getMessage());
        }
    }
}
