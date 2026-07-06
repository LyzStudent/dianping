package com.dianping.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.dianping.config.RabbitMQTopicConfig;
import com.dianping.entity.VoucherOrder;
import com.dianping.service.SeckillVoucherService;
import com.dianping.service.VoucherOrderService;
import com.dianping.service.VoucherService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息消费者
 */
@Slf4j
@Service
public class MQReceiver {

    @Resource
    VoucherOrderService voucherOrderService;

    @Resource
    SeckillVoucherService seckillVoucherService;

    @Transactional
    @RabbitListener(queues = RabbitMQTopicConfig.QUEUE)
    public void receiveSeckillMessage(String msg){
        log.info("接收到消息："+msg);
        VoucherOrder voucherOrder= JSON.parseObject(msg,VoucherOrder.class);

        Long voucherId=voucherOrder.getVoucherId();
        //1.一人一单
        Long userId=voucherOrder.getUserId();
        //2.查询订单
        long count=voucherOrderService.query().eq("user_id",userId).eq("voucher_id",voucherId).count();
        //3.判断是否存在
        if(count>0){
            //用户已经购买过了
            log.error("用户已经购买过了");
            return ;
        }
        log.info("扣减库存");
        //4.扣减库存
        boolean success=seckillVoucherService
                .update()
                .setSql("stock=stock-1")
                .eq("voucher_id",voucherId)
                .gt("stock",0)
                .update();
        if(!success){
            log.error("库存不足");
            return ;
        }
        //5.直接保存订单
        voucherOrderService.save(voucherOrder);
    }

}
