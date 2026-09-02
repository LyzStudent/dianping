package com.dianping.shop.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.common.dto.Result;
import com.dianping.shop.entity.Banner;
import com.dianping.shop.mapper.BannerMapper;
import com.dianping.shop.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.dianping.common.util.RedisContants.CACHE_BANNER_KEY;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBannerList() {
        //1.先查redis缓存
        List<String> cached=stringRedisTemplate.opsForList().range(CACHE_BANNER_KEY,0,-1);
        if(cached!=null&&!cached.isEmpty()){
            List<Banner> banners=new ArrayList<>();
            for(String json:cached){
                banners.add(JSONUtil.toBean(json,Banner.class));
            }
            return Result.ok(banners);
        }

        //2.缓存未命中查库
        List<Banner> banners=query().orderByAsc("sort").list();
        if(banners==null){
            banners=new ArrayList<>();
        }

        //3.回填缓存
        List<String> jsonList=new ArrayList<>();
        for(Banner b:banners){
            jsonList.add(JSONUtil.toJsonStr(b));
        }
        stringRedisTemplate.opsForList().leftPushAll(CACHE_BANNER_KEY,jsonList);
        return Result.ok(banners);
    }
}
