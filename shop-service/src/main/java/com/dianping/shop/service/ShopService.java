package com.dianping.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.Result;
import com.dianping.shop.entity.Shop;

public interface ShopService extends IService<Shop> {

    /**
     * 根据id查询店铺信息
     * @param id
     * @return
     */
    Result queryById(Long id);

    /**
     * 更新店铺信息
     * @param shop
     * @return
     */
    Result update(Shop shop);

    /**
     * 根据店铺类型分页查询
     * @param typeId
     * @param current
     * @param x
     * @param y
     * @return
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);


    /**
     * 创建/更新自己的店铺
     * @param shop
     * @return
     */
    Result merchantSaveShop(Shop shop);

    /**
     * 清除店铺缓存（管理员审核后调用，避免脏缓存）
     * @param id
     */
    void clearShopCache(Long id);

    void saveShopGeo(Long shopId,Long typeId,Double x,Double y);
    Result rebuildGeo();//全量重建
    void removeShopGeo(Long shopId,Long typeId);
}
