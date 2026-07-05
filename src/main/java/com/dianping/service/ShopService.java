package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Shop;

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
    Result queryShopByTyoe(Integer typeId, Integer current, Double x, Double y);
}
