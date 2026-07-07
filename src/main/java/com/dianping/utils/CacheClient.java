package com.dianping.utils;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dianping.dto.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.dianping.utils.RedisContants.CACHE_NULL_TTL;
import static com.dianping.utils.RedisContants.LOCK_SHOP_KEY;

//Redis缓存工具类，解决缓存穿透和缓存击穿问题
@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);


    //普通缓存写入
    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    //逻辑过期写入
    public void setWithLogicExpire(String key,Object value,Long time,TimeUnit timeUnit){
        RedisData<Object> redisData=new RedisData<>();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //缓存穿透问题解决
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit timeUnit){
        //先从redis中查
        String key=keyPrefix+id;
        String json=stringRedisTemplate.opsForValue().get(key);

        //如果不为空，则转为R类型直接返回
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }

        //否则从数据库中查
        R r=dbFallback.apply(id);

        //查不到，则将空值写入redis
        if(r==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }

        //查到了则转为json字符串
        String jsonStr=JSONUtil.toJsonStr(r);

        //存入redis，并设置ttl
        this.set(key,jsonStr,time,timeUnit);

        //返回最终结果
        return r;
    }

    //互斥锁解决缓存击穿问题
    public <R,ID> R queryWithMetux(String keyPrefix,ID id,Class<R> type,Function<ID,R> dbfallback,Long time,TimeUnit timeUnit){
        //先从redis中查
        String key=keyPrefix+id;
        String json=stringRedisTemplate.opsForValue().get(key);

        //如果不为空则转为R类型直接返回
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json,type);
        }
        if(json!=null){
            return null;
        }

        //否则去数据库中查
        R r=null;
        String lockKey=LOCK_SHOP_KEY+id;
        try {
            boolean flag = tryLock(lockKey);
            if (!flag) {
                Thread.sleep(50);
                return queryWithMetux(keyPrefix, id, type, dbfallback, time, timeUnit);
            }
            r = dbfallback.apply(id);
            //查不到，则将空值写入redis
            if (r == null) {
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
                return null;
            }
            //并存入redis，设置ttl
            this.set(key, r, time, timeUnit);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            unLock(lockKey);
        }
        return r;

    }

    //逻辑过期解决缓存击穿
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit){
        //1.先从redis中查
        String key=keyPrefix+id;
        String json=stringRedisTemplate.opsForValue().get(key);

        //2.如果未命中则返回空
        if(StrUtil.isNotBlank(json)){
            return null;
        }
        //3.命中,转为json反序列化对象
        RedisData redisData=JSONUtil.toBean(json, RedisData.class);
        R r=JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime=redisData.getExpireTime();

        //4.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.未过期，直接返回信息
            return r;
        }
        //6.过期，尝试获取互斥锁
        String lockKey=LOCK_SHOP_KEY+id;
        boolean flag=tryLock(key);
        //7.获取到了锁
        if(flag){
            //开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try{
                    R tmp=dbFallback.apply(id);
                    this.setWithLogicExpire(key,tmp,time,timeUnit);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
            //直接返回商铺信息
            return r;
        }
        //10.未获取到锁，直接返回商铺信息
        return r;
    }

    //获得互斥锁
    private boolean tryLock(String key){
        boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
