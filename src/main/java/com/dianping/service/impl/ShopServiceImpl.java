package com.dianping.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.mapper.ShopMapper;
import com.dianping.service.ShopService;
import com.dianping.utils.RedisData;
import com.dianping.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisContants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //下面出现缓存击穿问题，需要声明一个线程池来完成重构缓存
    private static final ExecutorService CACHE_REDUILD_EXECUTOE = Executors.newFixedThreadPool(10);

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透的代码逻辑
        Shop shop = queryWithChuantou(id);

        //利用互斥锁解决缓存击穿
        //Shop shop=queryWithjichuan_mutex(id);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    //解决缓存穿透的代码逻辑
    public Shop queryWithChuantou(Long id) {
        //先从redis中查，这里的常量是固定的前缀+店铺id
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //如果不为空（查询到了），则转为Shop类型直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //如果这个数据不存在，将这个数据写入Redis中，并且将value设置为空字符串，然后设置一个较短的TTL，返回错误信息
        //当再次发起查询时，先去redis中判断value是否为空字符串，如果是，则说明是我们刚刚存的不存在的数据，直接返回错误消息

        //如果查询到的是空字符串，则说明是我们缓存的空数据
        if ("".equals(shopJson)) {
            return null;
        }

        //不是则去数据库中查
        Shop shop = getById(id);
        //查不到则将空字符串写入Redis
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, null, CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //查到了转为json字符串
        String jsonStr = JSONUtil.toJsonStr(shop);

        //并存入redis中，设置ttl，防止存了错的缓存
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //最中将查询到的商户信息返回给前端
        return shop;
    }

    //互斥锁解决缓冲击穿
    public Shop queryWithjichuan_mutex(Long id) {
        //先从redis中查，这里的常量是固定的前缀+店铺id
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //如果不为空，则转为Shop类型直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //如果查询到的是空字符串，则说明是我们缓存的空数据
        if ("".equals(shopJson)) {
            return null;
        }

        //实现在高并发的情况下缓存重建
        Shop shop = null;
        try {
            //1.获取互斥锁
            boolean flag = tryLock(LOCK_SHOP_KEY + id);

            //2.失败，则休眠并重试
            while (!flag) {
                Thread.sleep(50);
                return queryWithjichuan_mutex(id);
            }

            //3.获取成功则读取数据库，重建缓存
            shop = getById(id);

            //查不到，则将空值写入Redis
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //查到了则转为json字符串
            String jsonStr = JSONUtil.toJsonStr(shop);

            //并存入redis中，设置ttl
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, jsonStr, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            //把最终查询到的商户信息返回给前端
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(LOCK_SHOP_KEY + id);
        }
        return shop;
    }

    //逻辑过期解决缓存击穿
    public Shop queryWithlogicExpire(Long id) {
        //1.从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        //2.如果未命中，则返回空
        if (StrUtil.isBlank(json)) {
            return null;
        }

        //3.命中，将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);

        //3.1将data转为shop对象
        Shop shop = JSONUtil.toBean(redisData.getData().toString(), Shop.class);

        //3.2获取过期时间
        LocalDateTime expireTime = redisData.getExpireTime();

        //4.判断是否过期
        //5.未过期，直接返回商铺信息
        if (LocalDateTime.now().isBefore(expireTime)) {
            return shop;
        }

        //6.过期，尝试获取互斥锁
        boolean flag = tryLock(LOCK_SHOP_KEY + id);

        //7.获取到了锁，开启独立线程，返回商铺信息
        if (flag) {
            CACHE_REDUILD_EXECUTOE.submit(() -> {
                try {
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(LOCK_SHOP_KEY + id);
                }
            });
            return shop;
        }
        //8.未获得锁直接返回商铺信息
        return shop;
    }

    //获得锁的代码逻辑
    private boolean tryLock(String key) {
        boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }

    //释放锁
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    //逻辑过期实现缓存击穿问题
    public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException {
        Shop shop=getById(id);
        Thread.sleep(200);//模拟上面取数据的时间

        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 更新商铺信息
     * @param shop
     * @return
     */
    @Override
    public Result update(Shop shop) {
        //首先先判断是否为空
        if(shop.getId()==null){
            return Result.fail("店铺id不能为空");
        }
        //先修改数据库
        updateById(shop);
        //再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryShopByTyoe(Integer typeId, Integer current, Double x, Double y) {
        //1.判断是否需要根据距离查询
        if(x==null||y==null){
            //根据类型分页查询
            Page<Shop> page=query()
                    .eq("type_id",typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            //返回数据
            return Result.ok(page.getRecords());
        }

        //2.计算分页查询参数
        int from=(current-1)*SystemConstants.MAX_PAGE_SIZE;
        int end = current*SystemConstants.MAX_PAGE_SIZE;

        String key=SHOP_GEO_KEY+typeId;

        //3.查询redis、按照距离排序、分页；结果：shopId、distance
        GeoResults<RedisGeoCommands.GeoLocation<String>> results=stringRedisTemplate.opsForGeo().search(key,
                GeoReference.fromCoordinate(x,y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));

        if(results==null){
            return Result.ok(Collections.emptyList());
        }

        //4.解析出id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list=results.getContent();

        if(list.size()<from){
            //起始查询位置大于数据总量，则说明没数据了，返回空集合
            return Result.ok(Collections.emptyList());
        }

        ArrayList<Long> ids=new ArrayList<>(list.size());
        HashMap<String,Distance> distanceHashMap=new HashMap<>(list.size());
        list.stream().skip(from).forEach(result->{
            String shopIdStr=result.getContent().getName();
            ids.add((Long.valueOf(shopIdStr)));
            Distance distance=result.getDistance();
            distanceHashMap.put(shopIdStr,distance);
        });

        //5.根据id查询shop
        String idsStr=StrUtil.join(",",ids);

        List<Shop> shops=query().in("id",ids).last("ORDER BY FIELD(id,"+idsStr+")").list();
        for(Shop shop:shops){
            //设置shop的举例属性，从distanceMao中根据shopId查询
            shop.setDistance(distanceHashMap.get(shop.getId().toString()).getValue());
        }

        //6.返回
        return Result.ok(shops);
    }
}
