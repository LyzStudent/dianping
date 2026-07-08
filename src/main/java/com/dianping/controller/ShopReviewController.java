package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.ShopReview;
import com.dianping.service.ShopReviewService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop-review")
public class ShopReviewController {

    @Resource
    private ShopReviewService shopReviewService;

    /**
     * 发表评论
     * @param shopReview
     * @return
     */
    @PostMapping
    public Result saveReview(@RequestBody ShopReview shopReview) {
        return shopReviewService.saveReview(shopReview);
    }

    /**
     * 修改评论
     * @param shopReview
     * @return
     */
    @PutMapping
    public Result updateReview(@RequestBody ShopReview shopReview){
        return shopReviewService.updateReview(shopReview);
    }

    /**
     * 删除评论
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public Result deleteReview(@PathVariable("id") Long id){
        return shopReviewService.deleteReview(id);
    }


    /**
     * 查询店铺的评论列表
     * @param shopId
     * @param current
     * @return
     */
    @GetMapping("/of/shop")
    public Result queryReviewsByShopId(
            @RequestParam("shopId") Long shopId,
            @RequestParam(value = "current",defaultValue = "1") Integer current
    ){
        return shopReviewService.queryReviewsByShopId(shopId,current);
    }

    /**
     * 查询当前用户评论该店铺
     * @param shopId
     * @return
     */
    @GetMapping("/my")
    public Result queryMyReview(@RequestParam("shopId") Long shopId){
        return shopReviewService.queryMyReview(shopId);
    }
}
