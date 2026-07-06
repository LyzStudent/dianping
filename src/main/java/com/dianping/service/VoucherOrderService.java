package com.dianping.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;
import org.springframework.stereotype.Service;

@Service
public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
