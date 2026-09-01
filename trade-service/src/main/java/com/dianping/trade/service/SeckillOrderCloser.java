package com.dianping.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dianping.trade.constant.OrderStatus;
import com.dianping.trade.entity.VoucherOrder;
import com.dianping.trade.mapper.VoucherOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.dianping.common.util.RedisContants.SECKILL_ORDER_KEY;
import static com.dianping.common.util.RedisContants.SECKILL_STOCK_KEY;

@Slf4j
@Component
public class SeckillOrderCloser {

    private final VoucherOrderMapper voucherOrderMapper;
    private final SeckillVoucherService seckillVoucherService;
    private final StringRedisTemplate stringRedisTemplate;

    public SeckillOrderCloser(VoucherOrderMapper voucherOrderMapper,
                              SeckillVoucherService seckillVoucherService,
                              StringRedisTemplate stringRedisTemplate) {
        this.voucherOrderMapper = voucherOrderMapper;
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 关闭单个超时订单并补回库存
     * 只有status=1的订单才真正关闭：并发支付/关闭只有一个成功
     */
    @Transactional
    public void closeOne(VoucherOrder order) {
        Long orderId = order.getId();
        //1.乐观关闭订单（等价于 UPDATE ... SET status=4 WHERE id=? AND status=1）
        boolean closed=voucherOrderMapper.update(null,
                Wrappers.<VoucherOrder>lambdaUpdate()
                        .set(VoucherOrder::getStatus, OrderStatus.CANCEL)
                        .set(VoucherOrder::getUpdateTime, LocalDateTime.now())
                        .eq(VoucherOrder::getId, orderId)
                        .eq(VoucherOrder::getStatus, OrderStatus.NOT_PAY))>0;
        if(!closed){
            log.error("订单 {} 已被并发处理，跳过回补",orderId);
            return;
        }
        //2.回补数据库库存
        Long voucherId=order.getVoucherId();
        seckillVoucherService.update()
                .setSql("stock=stock+1")
                .eq("voucher_id",voucherId)
                .update();
        //3.回补Redis库存+移除用户购买资格
        stringRedisTemplate.opsForValue().increment(SECKILL_STOCK_KEY+voucherId);
        stringRedisTemplate.opsForSet().remove(SECKILL_ORDER_KEY+voucherId,order.getUserId().toString());

        log.info("订单 {} 已关闭，库存已回补，voucherId={},userId={}",orderId,voucherId,order.getUserId());
    }
}
