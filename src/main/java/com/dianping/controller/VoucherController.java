package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.Voucher;
import com.dianping.service.VoucherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    /**
     * 新增普通卷
     * @param voucher 优惠卷信息
     * @return 优惠卷id
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher){
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增秒杀卷
     * @param voucher
     * @return
     */
    public Result addSeckillVoucher(@RequestBody Voucher voucher){
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId
     * @return
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId){
        return voucherService.queryVoucherOfShop(shopId);
    }
}
