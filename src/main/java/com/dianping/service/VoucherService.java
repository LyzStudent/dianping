package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Voucher;

public interface VoucherService extends IService<Voucher> {

    /**
     * 新增秒杀券
     * @param voucher
     */
    void addSeckillVoucher(Voucher voucher);


    /**
     * 查询店铺的优惠券信息
     * @param shopId
     * @return
     */
    Result queryVoucherOfShop(Long shopId);
}
