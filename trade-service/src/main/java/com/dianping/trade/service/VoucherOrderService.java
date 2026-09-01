package com.dianping.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.Result;
import com.dianping.trade.entity.VoucherOrder;

public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    /**
     * 模拟支付
     */
    Result payOrder(Long userId,Long orderId);

    /**
     * 关闭超时未支付订单
     */
    void closeTimeoutOrders(int timeoutMinutes);

    /**
     * 商家：查某店铺名下的所有订单
     */
    Result queryMerchantOrders(Long shopId);

    Result myOrders(Integer current);
    Result myVouchers();
    Result userOrders(Long userId,Long orderId);
}
