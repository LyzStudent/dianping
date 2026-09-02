package com.dianping.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.Result;
import com.dianping.shop.entity.Banner;

public interface BannerService extends IService<Banner> {
    Result queryBannerList();
}
