package com.dianping.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.Result;
import com.dianping.shop.entity.ShopType;

public interface ShopTypeService extends IService<ShopType> {
    Result queryList();
}
