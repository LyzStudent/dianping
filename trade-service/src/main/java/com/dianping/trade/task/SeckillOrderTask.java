package com.dianping.trade.task;

import com.dianping.trade.service.VoucherOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillOrderTask {

    @Resource
    private VoucherOrderService voucherOrderService;

    @Value("${seckill.order.ttl-minutes:15}")
    private int ttlMinutes;

    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void closeTimeoutOrders(){
        log.info("开始扫描超时未支付订单,超时阈值={}分钟",ttlMinutes);
        voucherOrderService.closeTimeoutOrders(ttlMinutes);
    }
}
