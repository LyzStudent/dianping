package com.dianping.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.common.util.UserHolder;
import com.dianping.shop.entity.Shop;
import com.dianping.shop.entity.UserFavorite;
import com.dianping.shop.mapper.UserFavoriteMapper;
import com.dianping.shop.service.ShopService;
import com.dianping.shop.service.UserFavoriteService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite>implements UserFavoriteService {

    private final ShopService shopService;

    public UserFavoriteServiceImpl(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public boolean toggle(Long shopId) {
        Long userId= UserHolder.getUser().getId();
        LambdaQueryWrapper<UserFavorite> w=new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId,userId)
                .eq(UserFavorite::getShopId,shopId);
        UserFavorite f=getOne(w);
        if(f==null){
            save(new UserFavorite().setUserId(userId).setShopId(shopId));
            return true;
        }else{
            removeById(f.getId());
            return false;
        }
    }

    @Override
    public boolean isFavorite(Long shopId) {
        Long userId=UserHolder.getUser().getId();
        return count(new LambdaUpdateWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId,userId)
                .eq(UserFavorite::getShopId,shopId))>0;
    }

    @Override
    public Long favoriteCount(Long shopId) {
        return count(new LambdaUpdateWrapper<UserFavorite>().eq(UserFavorite::getShopId,shopId));
    }

    @Override
    public List<Shop> myFavorites(Integer current) {
        Long userId=UserHolder.getUser().getId();
        Page<UserFavorite> page=lambdaQuery()
                .eq(UserFavorite::getUserId,userId)
                .orderByDesc(UserFavorite::getCreateTime)
                .page(new Page<>(current,10));
        List<Long> ids=page.getRecords().stream().map(UserFavorite::getShopId).toList();
        if(ids.isEmpty()){
            return Collections.emptyList();
        }
        return shopService.listByIds(ids);
    }
}
