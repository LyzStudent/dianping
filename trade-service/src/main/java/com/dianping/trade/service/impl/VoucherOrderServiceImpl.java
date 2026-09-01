package com.dianping.trade.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.common.dto.Result;
import com.dianping.common.feign.ShopClient;
import com.dianping.common.util.RedisIdWorker;
import com.dianping.common.util.UserHolder;
import com.dianping.trade.constant.OrderStatus;
import com.dianping.trade.entity.Voucher;
import com.dianping.trade.entity.VoucherOrder;
import com.dianping.trade.mapper.VoucherOrderMapper;
import com.dianping.trade.rabbitmq.MQSender;
import com.dianping.trade.service.SeckillOrderCloser;
import com.dianping.trade.service.VoucherOrderService;
import com.dianping.trade.service.VoucherService;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private MQSender mqSender;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SeckillOrderCloser seckillOrderCloser;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private VoucherService voucherService;

    @Resource
    private ShopClient shopClient;

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

    @Override
    public Result payOrder(Long userId, Long orderId) {
        VoucherOrder order=getById(orderId);

        if(order==null){
            return Result.fail("订单不存在");
        }

        if(!order.getUserId().equals(userId)){
            return Result.fail("无权操作该订单");
        }

        //乐观锁：where status=1,保证并发下只有一次能支付成功
        boolean ok=update()
                .setSql("status="+ OrderStatus.PAID+",pay_time=NOW(),update_time=NOW()")
                .eq("id",orderId)
                .eq("status",OrderStatus.NOT_PAY)
                .update();
        return ok?Result.ok():Result.fail(
                order.getStatus()==OrderStatus.PAID?"订单已支付":"订单已关闭"
        );
    }

    @Override
    public void closeTimeoutOrders(int timeoutMinutes) {
        LocalDateTime deadLine=LocalDateTime.now().minusMinutes(timeoutMinutes);

        //分布式锁:多实例部署时保证扫描任务只被一个示例执行
        RLock lock=redissonClient.getLock("lock:seckill:close-scan");
        try{
            if(!lock.tryLock(0,60,TimeUnit.SECONDS)){
                return;//其他实例正在扫描
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return;
        }
        try{
            //分页扫描超时未支付订单
            long current=1;
            while (true) {
                Page<VoucherOrder> page=query()
                        .eq("status",OrderStatus.NOT_PAY)
                        .lt("create_time",deadLine)
                        .page(new Page<>(current,200));
                List<VoucherOrder> orders=page.getRecords();
                if(orders.isEmpty()){
                    break;
                }
                for(VoucherOrder order:orders){
                    seckillOrderCloser.closeOne(order);
                }
                if(current>=page.getPages()){
                    break;
                }
                current++;
            }
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    /**
     * 商家：查某店铺名下的所有订单
     * 先查该店铺的券id，再查这些券的订单（倒序）
     */
    @Override
    public Result queryMerchantOrders(Long shopId) {
        Long userId=UserHolder.getUser().getId();
        //越权校验:店铺必须属于当前商家
        Result owner=shopClient.getShopOwner(shopId);
        if(owner.getSuccess()==null||!owner.getSuccess()){
            return Result.fail("店铺不存在");
        }
        //店铺无归属（如C端匿名创建）时 ownerId 为 null，同样视为无权查看
        Object ownerId=owner.getData();
        if(ownerId==null||userId.longValue()!=((Number)ownerId).longValue()){
            return Result.fail("无权查看该店铺的订单");
        }

        List<Voucher> vouchers = voucherService.query().eq("shop_id", shopId).list();
        if (vouchers.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> vids = vouchers.stream().map(Voucher::getId).toList();
        List<VoucherOrder> orders = query().in("voucher_id", vids).orderByDesc("create_time").list();
        return Result.ok(orders);
    }

    /**
     * 我的订单（分页）
     * @param current
     * @return
     */
    @Override
    public Result myOrders(Integer current) {
        Long userId=UserHolder.getUser().getId();
        //1.分页查自己的订单
        Page<VoucherOrder> page=query()
                .eq("user_id",userId)
                .orderByDesc("create_time")
                .page(new Page<>(current,10));

        //2.批量查券填充标题
        List<Long> vids=page.getRecords().stream().map(VoucherOrder::getVoucherId).toList();
        Map<Long,Voucher> vmap=vids.isEmpty()?Collections.emptyMap():voucherService.listByIds(vids).stream().collect(Collectors.toMap(Voucher::getId,v->v));
        //3.组装订单
        List<Map<String , Object>> records=page.getRecords().stream().map(order -> {
            Map<String,Object> m=new HashMap<>();
            m.put("orderId",order.getId());
            m.put("voucherId",order.getVoucherId());
            m.put("status",order.getStatus());
            m.put("createTime",order.getCreateTime());
            m.put("payTime",order.getPayTime());
            m.put("useTime",order.getUseTime());
            m.put("voucher",vmap.get(order.getVoucherId()));
            return m;
        }).toList();

        Map<String,Object> data=new HashMap<>();
        data.put("total",page.getTotal());
        data.put("records",records);
        return Result.ok(data);
    }

    /**
     * 券包：已支付未核销
     * @return
     */
    @Override
    public Result myVouchers() {
        Long userId=UserHolder.getUser().getId();
        //1.查自己已支付未核销的券（status=2）
        List<VoucherOrder> orders=query()
                .eq("user_id",userId)
                .eq("status",OrderStatus.PAID)
                .orderByDesc("pay_time")
                .list();

        //2.批量查券填充标题，和 myOrders 结构保持一致
        List<Long> vids=orders.stream().map(VoucherOrder::getVoucherId).toList();
        Map<Long,Voucher> vmap=vids.isEmpty()?Collections.emptyMap():voucherService.listByIds(vids).stream().collect(Collectors.toMap(Voucher::getId,v->v));

        List<Map<String,Object>> records=orders.stream().map(order -> {
            Map<String,Object> m=new HashMap<>();
            m.put("orderId",order.getId());
            m.put("voucherId",order.getVoucherId());
            m.put("status",order.getStatus());
            m.put("createTime",order.getCreateTime());
            m.put("payTime",order.getPayTime());
            m.put("useTime",order.getUseTime());
            m.put("voucher",vmap.get(order.getVoucherId()));
            return m;
        }).toList();
        return Result.ok(records);
    }

    /**
     * 核销：状态机已支付（2）->已核销（3）
     * @param userId
     * @param orderId
     * @return
     */
    @Override
    public Result userOrders(Long userId, Long orderId) {
        VoucherOrder order=getById(orderId);
        if(order==null){
            return Result.fail("券不存在");
        }
        if(!order.getUserId().equals(userId)){
            return Result.fail("无权核销该券");
        }

        //乐观锁保证只有一次核销成功
        boolean ok=update()
                .setSql("status="+OrderStatus.USED+",use_time=NOW(),update_time=NOW()")
                .eq("id",orderId)
                .eq("status",OrderStatus.PAID)
                .update();
        return ok?Result.ok():Result.fail(
                order.getStatus()==OrderStatus.USED?"该券已核销":"当前状态不允许核销"
        );
    }
}
