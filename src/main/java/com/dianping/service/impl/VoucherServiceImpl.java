package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.SeckillVoucher;
import com.dianping.entity.Voucher;
import com.dianping.mapper.VoucherMapper;
import com.dianping.service.SeckillVoucherService;
import com.dianping.service.VoucherService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.dianping.utils.RedisContants.SECKILL_STOCK_KEY;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    @Resource
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加秒杀券
     * @param voucher
     */
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        //保存优惠卷到普通优惠卷voucher数据库
        save(voucher);

        //保存秒杀信息
        SeckillVoucher seckillVoucher=new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());

        //把秒杀信息写入缓存，否则执行seckill.lua的时候找不到缓存，导致与空值相比而报错
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY+voucher.getId(),voucher.getStock().toString());
        seckillVoucherService.save(seckillVoucher);
    }


    /**
     * 查询店铺的优惠券信息
     * @param shopId
     * @return
     */
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        //查询优惠卷信息
        List<Voucher> vouchers=getBaseMapper().queryVoucherOfShop(shopId);

        //返回结果
        return Result.ok(vouchers);
    }
}
