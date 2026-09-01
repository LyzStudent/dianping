package com.dianping.trade.controller;

import com.dianping.common.annotation.RequireRole;
import com.dianping.common.dto.Result;
import com.dianping.trade.entity.Voucher;
import com.dianping.trade.service.VoucherService;
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
    @RequireRole("2")
    public Result addVoucher(@RequestBody Voucher voucher){
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增秒杀卷
     * @param voucher
     * @return
     */
    @PostMapping("/seckill")
    @RequireRole("2")
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

    /**
     * 发券加权限
     * @param voucher
     * @return
     */
    @PostMapping("/merchant/voucher")
    @RequireRole("2")
    public Result addMerchantVoucher(@RequestBody Voucher voucher){
        //校验shopId属于当前商家
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 加入秒杀券
     * @param voucher
     * @return
     */
    @PostMapping("/merchant/voucher/seckill")
    @RequireRole("2")
    public Result addMerchantSeckillVoucher(@RequestBody Voucher voucher){
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    @GetMapping("/merchant/voucher/list/{shopId}")
    @RequireRole("2")
    public Result merchantVoucherList(@PathVariable("shopId") Long shopId){
        return voucherService.queryVoucherOfShop(shopId);
    }
}
