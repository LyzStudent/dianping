package com.dianping.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.rabbitmq.MQSender;
import com.dianping.service.SeckillVoucherService;
import com.dianping.service.VoucherOrderService;
import com.dianping.utils.RedisIdWorker;
import com.dianping.utils.UserHolder;
import jakarta.annotation.Resource;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.PushBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.TimeUnit;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private MQSender mqSender;

    private RateLimiter rateLimiter=RateLimiter.create(10);

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
        //令牌桶算法，限流
        if(!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)){
            return Result.fail("目前网络正忙，请重试");
        }

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


//    /**
//     * 秒杀系统的同步下单方式
//     * @param voucherId
//     * @return
//     */
//    @Transactional
//    public Result createVoucherOrder(Long voucherId){
//        //一人一单逻辑
//        Long userId=UserHolder.getUser().getId();
//
//        Long count=query().eq("voucher_id",voucherId).eq("user_id",userId).count();
//        if(count>0){
//            return Result.fail("你已经抢过优惠卷了");
//        }
//
//        //扣减库存
//        boolean success= SeckillVoucherService.update()
//                .setSql("stock=stock-1")
//                .eq("voucher_id",voucherId)
//                .gt("stock",0) //加了CAS乐观锁，Compare and swap
//                .update();
//
//        if(!success){
//            return Result.fail("库存不足");
//        }
//
//
//        //创建订单
//        VoucherOrder voucherOrder=new VoucherOrder();
//
//        //设置订单id，生成订单的全局id
//        long orderId=redisIdWorker.nextId("order");
//
//        //设置用户id
//        Long id=UserHolder.getUser().getId();
//
//        //设置代金卷id
//        voucherOrder.setVoucherId(voucherId);
//        voucherOrder.setId(orderId);
//        voucherOrder.setUserId(id);
//
//        //将订单数据保存到表中
//        save(voucherOrder);
//
//        //返回订单
//        return Result.ok(orderId);
//    }

}
