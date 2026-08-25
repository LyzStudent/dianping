package com.dianping.trade.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.common.dto.Result;
import com.dianping.common.util.RedisIdWorker;
import com.dianping.common.util.UserHolder;
import com.dianping.trade.entity.VoucherOrder;
import com.dianping.trade.mapper.VoucherOrderMapper;
import com.dianping.trade.rabbitmq.MQSender;
import com.dianping.trade.service.VoucherOrderService;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private MQSender mqSender;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //lua脚本
    public static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 秒杀
     * @param voucherId
     * @return
     */

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.执行lua脚本
        Long userId= UserHolder.getUser().getId();

        Long r=stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );

        //2.判断结果为0，为0代表有购买资格，反之没有
        int result=r.intValue();
        if(result!=0){
            return Result.fail(r==1?"库存不足":"该用户重复下单");
        }

        //3.有资格则将下单消息保存到阻塞队列中
        //4.创建订单
        VoucherOrder voucherOrder=new VoucherOrder();

        //5.订单id
        long orderId=redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);

        //6.用户id
        voucherOrder.setUserId(userId);

        //7.代金卷id
        voucherOrder.setVoucherId(voucherId);

        //8.将信息放入MQ中
        mqSender.sendSeckillMessage(JSON.toJSONString(voucherOrder));

        //9.返回订单id
        return Result.ok(orderId);
//        //单机模式下，使用synchronized实现锁
//        synchronized (userId.toString().intern()){
//            //需要使用代理来生效，需要获得原始的事务对象来操作事务
//            return VoucherOrderService.createVoucherOrder(voucherId);
//        }
    }
}
