package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.ShopReview;

public interface ShopReviewService extends IService<ShopReview> {

    // 发表评论
    Result saveReview(ShopReview shopReview);

    // 修改评论
    Result updateReview(ShopReview shopReview);

    // 删除评论
    Result deleteReview(Long id);

    // 查询店铺的评论列表
    Result queryReviewsByShopId(Long shopId, Integer current);

    // 查询当前用户评论该店铺
    Result queryMyReview(Long shopId);
}
