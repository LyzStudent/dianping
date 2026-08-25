package com.dianping.trade.controller;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.dianping.common.dto.Result;
import com.dianping.trade.service.VoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoucherOrderController {

    @Autowired
    private VoucherOrderService voucherOrderService;


    /**
     * 秒杀
     * @param voucherId
     * @return
     */
    @PostMapping("/seckill/{id}")
    @SentinelResource(value = "seckill",blockHandler = "seckillBlockHandler")
    public Result seckillVoucher(@PathVariable("id") Long voucherId){
        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 触发限流时返回（不抛异常）
     * @param voucherId
     * @param e
     * @return
     */
    public Result seckillBlockHandler(Long voucherId, BlockException e){
        return Result.fail("系统繁忙，请稍后再试");
    }
}
