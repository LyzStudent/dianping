package com.dianping.trade.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.dianping.common.dto.Result;
import com.dianping.common.feign.UserClient;
import com.dianping.trade.config.RabbitMQTopicConfig;
import com.dianping.trade.entity.VoucherOrder;
import com.dianping.trade.service.SeckillVoucherService;
import com.dianping.trade.service.VoucherOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
    @Autowired
    private UserClient userClient;

    @GlobalTransactional(name = "seckill-create-order",rollbackFor = Exception.class)
    @Transactional
    @RabbitListener(queues = RabbitMQTopicConfig.QUEUE)
    public void receiveSeckillMessage(String msg){
        log.info("接收到消息："+msg);
        VoucherOrder voucherOrder= JSON.parseObject(msg,VoucherOrder.class);

        Long voucherId=voucherOrder.getVoucherId();
        //1.一人一单
        Long userId=voucherOrder.getUserId();
        //2.查询订单
        long count=voucherOrderService.query()
                .eq("user_id",userId)
                .eq("voucher_id",voucherId)
                .in("status",1,2,3)//只有 未支付/已支付/已核销才算买过；关闭/退款不算
                .count();
        //3.判断是否存在
        if(count>0){
            //用户已经购买过了
            log.warn("用户已经购买过了,userId={}",userId);
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
            log.warn("库存不足,voucherId={}",voucherId);
            return ;
        }
        //5.直接保存订单(唯一索引防并发重复)
        try {
            voucherOrderService.save(voucherOrder);
        }catch (DuplicateKeyException e){
            log.warn("重复下单被唯一索引拦截，user={},voucher={}",userId,voucherId);
            return;
        }

        //6.跨服务加积分-失败则抛异常，整个全局事务回滚
        Result result=userClient.addPoints(userId,10);
        if(result==null||!Boolean.TRUE.equals(result.getSuccess())){
            throw new RuntimeException("加积分失败，全局事务回滚");
        }
    }

}
