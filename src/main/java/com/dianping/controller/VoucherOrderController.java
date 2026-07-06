package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.service.VoucherOrderService;
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
    public Result seckillVoucher(@PathVariable("id") Long voucherId){
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
