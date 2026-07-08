package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Shop;
import com.dianping.entity.ShopReview;
import com.dianping.entity.User;
import com.dianping.mapper.ShopReviewMapper;
import com.dianping.service.ShopReviewService;
import com.dianping.service.ShopService;
import com.dianping.service.UserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopReviewServiceImpl extends ServiceImpl<ShopReviewMapper, ShopReview> implements ShopReviewService {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    //查询当前用户评论该店铺
    @Override
    public Result queryMyReview(Long shopId) {
        UserDTO userDTO=UserHolder.getUser();

        ShopReview shopReview=query()
                .eq("shop_id",shopId)
                .eq("user_id",userDTO.getId())
                .one();

        return Result.ok(shopReview);
    }

    //查询店铺的评论列表
    @Override
    public Result queryReviewsByShopId(Long shopId, Integer current) {
        Page<ShopReview> page=query()
                .eq("shop_id",shopId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<ShopReview> reviews=page.getRecords();

        //填充用户信息
        reviews.forEach(review->{
            User user=userService.getById(review.getUserId());
            if(user!=null){
                review.setNickName(user.getNickName());
                review.setIcon(user.getIcon());
            }
        });
        return Result.ok(reviews);
    }

    //删除评论
    @Override
    public Result deleteReview(Long id) {
        UserDTO userDTO=UserHolder.getUser();

        ShopReview shopReview=getById(id);
        if(shopReview==null){
            return Result.fail("评论不存在");
        }
        if(!shopReview.getUserId().equals(userDTO.getId())){
            return Result.fail("只能删除自己的评论");
        }

        boolean success=removeById(id);
        if(!success){
            return Result.fail("删除失败");
        }

        updateShopScore(shopReview.getShopId());

        return Result.ok();
    }

    // 修改评论
    @Override
    public Result updateReview(ShopReview shopReview) {
        UserDTO userDTO=UserHolder.getUser();

        ShopReview oldReview=getById(userDTO.getId());
        if(oldReview==null){
            return Result.fail("评论不存在");
        }
        if(!oldReview.getUserId().equals(userDTO.getId())){
            return Result.fail("只能修改自己的评论");
        }

        //防止前端篡改
        shopReview.setUserId(userDTO.getId());
        shopReview.setShopId(oldReview.getShopId());

        boolean success=updateById(shopReview);
        //评分变了进行重算
        if(!success){
            return Result.fail("修改失败");
        }
        if(!oldReview.getRating().equals(shopReview.getRating())){
            updateShopScore(oldReview.getShopId());
        }

        return Result.ok();
    }

    // 发表评论
    @Override
    public Result saveReview(ShopReview shopReview) {
        UserDTO userDTO= UserHolder.getUser();
        shopReview.setUserId(userDTO.getId());

        //校验评分
        if(shopReview.getRating()==null||shopReview.getRating()<1||shopReview.getRating()>5){
            return Result.fail("评分必须在1-5之间");
        }

        //校验内容
        if(shopReview.getContent().isBlank()){
            return Result.fail("评论内容不能为空");
        }

        //检查是否已经评论过
        Long count=query().eq("shop_id",shopReview.getShopId())
                .eq("user_id",userDTO.getId())
                .count();
        if(count>0){
            return Result.fail("您已经评论过该店铺");
        }

        boolean success=save(shopReview);
        if(!success){
            return Result.fail("评论失败");
        }

        //重算店铺评分
        updateShopScore(shopReview.getShopId());
        return Result.ok(shopReview.getId());
    }

    /**
     * 根据店铺id更新店铺评分
     * @param shopId
     */
    private void updateShopScore(Long shopId){
        List<ShopReview> reviews=query().eq("shop_id",shopId).list();

        int totalComments=reviews.size();
        int avgScore=0;
        if(totalComments>0){
            double avg=reviews.stream()
                    .mapToInt(ShopReview::getRating)
                    .average()
                    .orElse(0);
            avgScore=(int) Math.round(avg*10);
        }
        Shop shop=new Shop();
        shop.setId(shopId);
        shop.setScore(avgScore);
        shop.setComments(totalComments);
        shopService.queryById(shopId);
    }
}
