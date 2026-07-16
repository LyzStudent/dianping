package com.dianping.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.BlogComments;
import com.dianping.entity.User;
import com.dianping.mapper.BlogCommentsMapper;
import com.dianping.service.BlogCommentsService;
import com.dianping.service.BlogService;
import com.dianping.service.UserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements BlogCommentsService {


    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BlogService blogService;

    private static final String COMMENT_LIKED_KEY="comment:liked";

    /**
     * 发表评论
     * @param blogComments
     * @return
     */
    @Override
    public Result saveComments(BlogComments blogComments) {
        //1.获取当前登录用户
        UserDTO user= UserHolder.getUser();
        blogComments.setUserId(user.getId());

        //2.如果是一级评论，parentId和answerId都设为0
        if(blogComments.getParentId()==null){
            blogComments.setParentId(0L);
        }
        if(blogComments.getAnswerId()==null){
            blogComments.setAnswerId(0L);
        }

        //3.保存到数据库中
        boolean success=save(blogComments);
        if(!success){
            return Result.fail("发表评论失败");
        }

        blogService.update()
                .setSql("comment=comment+1")
                .eq("id",blogComments.getBlogId())
                .update();

        return Result.ok(blogComments.getId());
    }

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    @Override
    public Result deleteComment(Long commentId) {
        //1.查出评论
        BlogComments comment=getById(commentId);
        if(comment==null){
            return Result.fail("评论不存在");
        }

        //2.校验权限，只有评论本人可以删除
        Long currentUserId=UserHolder.getUser().getId();
        if(!comment.getUserId().equals(currentUserId)){
            return Result.fail("无权删除他人评论");
        }

        //3.删除
        boolean success=removeById(commentId);
        if(!success){
            return Result.fail("删除失败");
        }

        blogService.update()
                .setSql("comments=comments-1")
                .eq("id",comment.getBlogId())
                .update();

        return Result.ok();
    }

    /**
     * 根据博客id查询评论
     * @param blogId
     * @param current
     * @return
     */
    @Override
    public Result queryCommentsByBlogId(Long blogId, Integer current) {
        //1.分页查询一级评论
        Page<BlogComments> page=query()
                .eq("blog_id",blogId)
                .eq("parent_id",0)
                .eq("status",0)
                .orderByDesc("liked")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<BlogComments> records=page.getRecords();

        //2.查询每个一级评论的前3条回复
        for(BlogComments comment:records){
            //填充评论者信息
            fillUserInfo(comment);

            //查询该评论的回复（前3条）
            List<BlogComments> replies=query()
                    .eq("parent_id",comment.getId())
                    .eq("status",0)
                    .orderByAsc("create_time")
                    .last("LIMIT 3")
                    .list();
            //填充回复者的用户信息
            for(BlogComments reply:replies){
                fillUserInfo(reply);
                //填充被回复者的昵称
                if(!reply.getAnswerId().equals(0L)
                &&!reply.getAnswerId().equals(comment.getId())){
                    //answerId!=0且不等于一级评论id，说明是回复另一个二级评论
                    BlogComments answerComment=getById(reply.getAnswerId());
                    if(answerComment!=null){
                        User answerUser=userService.getById(answerComment.getUserId());
                        reply.setAnswerNickName(answerUser.getNickName());
                    }
                }
            }
            comment.setReplies(replies);
        }
        return Result.ok(records,page.getTotal());
    }

    /**
     * 查询某条一级评论的更多回复
     * @param parentId
     * @param current
     * @return
     */
    @Override
    public Result queryRepliesByParentId(Long parentId, Integer current) {
        Page<BlogComments> page=query()
                .eq("parent_id",parentId)
                .eq("status",0)
                .orderByAsc("create_time")
                .page(new Page<>(current,SystemConstants.MAX_PAGE_SIZE));

        List<BlogComments> records=page.getRecords();
        for(BlogComments reply:records){
            fillUserInfo(reply);

            if(!reply.getAnswerId().equals(0L)){
                BlogComments answerComment=getById(reply.getAnswerId());
                if(answerComment!=null){
                    User answerUser=userService.getById(answerComment.getUserId());
                    reply.setAnswerNickName(answerUser.getNickName());
                }
            }
        }
        return Result.ok(records,page.getTotal());
    }

    /**
     * 点赞/取消赞评论
     * @param commentId
     * @return
     */
    @Override
    public Result likeComment(Long commentId) {
        Long userId=UserHolder.getUser().getId();
        String key=COMMENT_LIKED_KEY+commentId;

        //用redis zset判断是否已点赞
        Double score=stringRedisTemplate.opsForZSet().score(key,userId.toString());

        if(score==null){
            //未点赞
            update().setSql("liked=liked+1").eq("id",commentId).update();
            stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
        }else{
            //已点赞
            update().setSql("like=like-1").eq("id",commentId).update();
            stringRedisTemplate.opsForZSet().remove(key,userId.toString());
        }

        return Result.ok();
    }

    /**
     * 填充评论者的昵称和头像
     */
    private void fillUserInfo(BlogComments comment){
        User user=userService.getById(comment.getUserId());
        if(user!=null){
            comment.setNickName(user.getNickName());
            comment.setIcon(user.getIcon());
        }
    }
}
