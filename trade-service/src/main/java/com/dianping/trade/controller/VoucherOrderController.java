package com.dianping.trade.controller;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.dianping.common.annotation.RequireRole;
import com.dianping.common.dto.Result;
import com.dianping.common.util.UserHolder;
import com.dianping.trade.service.VoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 模拟支付（余额支付）
     */
    @PostMapping("/voucher-order/{id}/pay")
    public Result pay(@PathVariable("id") Long orderId){
        return voucherOrderService.payOrder(UserHolder.getUser().getId(),orderId);
    }

    @GetMapping("/voucher-order/admin/stats/order-count")
    @RequireRole("3")
    public Result orderCount(){
        return Result.ok(voucherOrderService.count());
    }

    /**
     * 商家：查某店铺名下的订单
     */
    @GetMapping("/merchant/voucher-order/list")
    @RequireRole("2")
    public Result merchantOrders(@RequestParam("shopId") Long shopId){
        return voucherOrderService.queryMerchantOrders(shopId);
    }

    /**
     * 我的订单
     * @param current
     * @return
     */
    @GetMapping("/voucher-order/my")
    public Result myOrders(@RequestParam(value = "current",defaultValue = "1") Integer current){
        return voucherOrderService.myOrders(current);
    }

    /**
     * 券包：已支付未核销的券
     * @return
     */
    @GetMapping("/voucher-order/my/vouchers")
    public Result myVouchers(){
        return voucherOrderService.myVouchers();
    }

    /**
     * 核销：已支付->已核销
     * @param orderId
     * @return
     */
    @PostMapping("/voucher-order/use/{orderId}")
    public Result useOrder(@PathVariable("orderId") Long orderId){
        return voucherOrderService.userOrders(UserHolder.getUser().getId(), orderId);
    }
}
