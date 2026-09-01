package com.dianping.shop.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.shop.entity.Shop;
import com.dianping.shop.entity.UserFavorite;

import java.util.List;

public interface UserFavoriteService extends IService<UserFavorite> {

    boolean toggle(Long shopId);

    boolean isFavorite(Long shopId);

    Long favoriteCount(Long shopId);

    List<Shop> myFavorites(Integer current);
}
