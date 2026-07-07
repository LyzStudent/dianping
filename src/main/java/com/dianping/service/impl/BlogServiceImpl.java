package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.ScrollResult;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.BlogMapper;
import com.dianping.service.BlogService;
import com.dianping.service.FollowService;
import com.dianping.service.UserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisContants.BLOG_LIKED_KEY;
import static com.dianping.utils.RedisContants.FEED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

    @Resource
    private UserService userService;

    @Autowired
    private FollowService followService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 保存博客
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        //获取登录用户
        UserDTO userDto= UserHolder.getUser();
        blog.setUserId(userDto.getId());

        //保存探店博文
        boolean isSuccess=save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败!");
        }

        //如果保存成功，则获取保存笔记的发布者id，用该id区follow_user表中查对应的粉丝id
        //select * from tb_follow where follow_user_id = ？
        List<Follow> followUsers=followService.query().eq("follow_user_id",userDto.getId()).list();
        for(Follow follow:followUsers){
            Long userId=follow.getUserId();
            String key=FEED_KEY+userId;
            //推送数据，每个粉丝都有一个自己的收件箱
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        //返回id
        return Result.ok(blog.getId());
    }

    /**
     * 点赞
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        //1.获取当前用户
        Long userId=UserHolder.getUser().getId();

        //2.如果当前用户未点赞，则点赞数+1，同时将该用户加入set集合
        String key=BLOG_LIKED_KEY+id;
        //尝试获取score
        Double score=stringRedisTemplate.opsForZSet().score(key,userId.toString());
        //为null，则说明集合中没有该用户
        if(score==null){
            //点赞数+1
            boolean success=update().setSql("like=like+1").eq("id",id).update();
            //将用户加入set集合
            if(success){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }
        //3.如果当前用户已点赞，则点赞数-1，同时将用户从set集合移除
        else{
            //点赞数-1
            boolean success=update().setSql("like=like-1").eq("id",id).update();
            //从set集合中移除
            if(success){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询点赞
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Integer id) {
        String key=BLOG_LIKED_KEY+id;
        //查询点赞的前5个
        Set<String> top5=stringRedisTemplate.opsForZSet().range(key,0,4);
        //如果为空（可能没人点赞），返回空集合
        if (top5==null||top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids=top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //将ids使用`,`拼接，SQL语句查询出来的结果并不是按照我们期望的方式进行排
        //所以我们需要用order by field来指定排序方式，期望的排序方式就是按照查询出来的id进行排序
        String idsStr= StrUtil.join(",",ids);
        //selcet *from tb_user where id in (ids[0],ids[1]...) order by field(id,ids[0],ids[1]...)
        List<UserDTO> userDTOS=userService.query().in("id",ids)
                .last("order by field(id"+idsStr+")")
                .list().stream()
                .map(user -> BeanUtil.copyProperties(user,UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);

    }

    /**
     * 滚动分页查询关注博客动态
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1.获取当前用户
        Long userId=UserHolder.getUser().getId();

        //2.查询该用户收件箱
        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples=stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key,0,max,offset,2);
        //3.非空判断
        if(typedTuples==null||typedTuples.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        //4.解析数据，blogId、minTime（时间戳）、offset
        ArrayList<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for(ZSetOperations.TypedTuple<String> typedTuple:typedTuples){
            //4.1获取id
            String id=typedTuple.getValue();
            ids.add(Long.valueOf(id));

            //4.2获取score（时间戳）
            long time=typedTuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else{
                minTime=time;
                os=1;
            }
        }
        String idsStr=StrUtil.join(",");

        //5.根据id查询blog
        List<Blog> blogs=query().in("id",ids).last("ORDER BY FIELD(id"+idsStr+")").list();

        for(Blog blog:blogs){
            //5.1 查询发布该blog的用户信息
            queryBlogUser(blog);
            //5.2 查询当前用户是否给该blog点过赞
            isBlogLiked(blog);

        }

        //6.封装数据并返回
        ScrollResult scrollResult=new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);

    }

    private void queryBlogUser(Blog blog){
        Long userId=blog.getUserId();
        User user=userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void isBlogLiked(Blog blog){
        //1.获取当前用户信息
        UserDTO userDTO=UserHolder.getUser();

        //当用户未登录时，就不判断了，直接return结束逻辑
        if(userDTO==null){
            return;
        }

        //2.判断当前用户是否点赞
        String key=BLOG_LIKED_KEY+blog.getId();
        Double score=stringRedisTemplate.opsForZSet().score(key,userDTO.getId().toString());
        blog.setIsLike(score!=null);
    }

    /**
     * 查询热门博客
     * @param current
     * @return
     */
    @Override
    public Result queryHotBlog(Integer current) {
        //根据用户查询
        Page<Blog> page=query().orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        //获取当前页数据
        List<Blog> records=page.getRecords();

        //查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);
            //追加判断blog是否被当前用户点赞，逻辑封装到isBlogLiked方法中
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    @Override
    public Result queryById(Integer id) {
        Blog blog=getById(id);
        if(blog==null){
            return Result.fail("博客不存在或已经被删除");
        }
        queryBlogUser(blog);
        //追加判断blog是否被当前用户点赞，逻辑封装到isBlogLiked方法中
        isBlogLiked(blog);
        return Result.ok(blog);

    }
}
