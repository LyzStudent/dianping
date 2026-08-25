package com.dianping.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.Result;
import com.dianping.trade.entity.VoucherOrder;

public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
