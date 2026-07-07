package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Follow;
import com.dianping.mapper.FollowMapper;
import com.dianping.service.FollowService;
import com.dianping.service.UserService;
import com.dianping.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserService userService;

    //判断是否关注
    @Override
    public Result isFollow(Long followUserId){
        //获取当前登录的userId
        long userId=UserHolder.getUser().getId();
        //select * from tb_follow where user_id=? and follow_user_id=?
        Long count = query().eq("user_id",userId)
                .eq("follow_user_id",followUserId).count();
        return Result.ok(count>0);
    }

    //关注与取消关注
    @Override
    public Result follow(Long followUserId, Boolean isFellow) {
        //获取登录用户id
        Long userId=UserHolder.getUser().getId();
        String key="follow:"+userId;

        //判断是否关注
        if(isFellow){
            //关注，将信息保存到数据库中
            Follow follow=new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSucess=save(follow);
            //成功将数据保存到redis中
            if(isSucess){
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else{
            //取关，则将数据从数据库中移除
            //delete from tb_follow where user_id=? and follow_user_id=?
            boolean isSucess=remove(new QueryWrapper<Follow>().eq("user_id",userId)
                    .eq("follow_user_id",followUserId));
            //将数据从redis中移除
            if(isSucess){
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }

        //返回结果
        return Result.ok();
    }

    //共同关注
    @Override
    public Result followCommons(Long id) {
        //获取当前用户
        Long userId=UserHolder.getUser().getId();
        String key1="follow:"+id;
        String key2="follow:"+userId;

        //对当前用户和博主用户的关注列表取交集
        Set<String> intersect=stringRedisTemplate.opsForSet().intersect(key1,key2);
        if(intersect==null||intersect.isEmpty()){
            //无交集则说明没有共同关注，返回空集合
            return Result.ok(Collections.emptyList());
        }

        //结果转为list
        List<Long> ids=intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //之后根据ids取查询共同关注的用户，封装成UserDto再返回
        List<UserDTO> userDtos=userService.listByIds(ids).stream().map(user->
                BeanUtil.copyProperties(user,UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDtos);
    }
}
