package com.dianping.controller;


import cn.hutool.core.bean.BeanUtil;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.entity.UserInfo;
import com.dianping.service.UserInfoService;
import com.dianping.service.UserService;
import com.dianping.utils.UserHolder;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) throws MessagingException {
        return userService.sendcode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm,HttpSession session){
        //实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token){
        return userService.logout(token);
    }

    // 获取当前登录用户详情
    public Result me(){
        //获取当前登录用户并返回
        UserDTO user= UserHolder.getUser();
        return Result.ok(user);
    }


    // 查看当前用户详情
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        //查看详情
        UserInfo userInfo=userInfoService.getById(userId);
        if(userInfo==null){
            //如果没有详情则应该是第一次查看详情
            return Result.ok();
        }
        userInfo.setCreateTime(null);
        userInfo.setUpdateTime(null);
        //返回
        return Result.ok(userInfo);
    }


    // 查询用户详情
    public Result queryById(@PathVariable("id") Long userId){
        //查看详情
        User user=userService.getById(userId);
        if(user==null){
            return Result.ok();
        }

        UserDTO userDTO= BeanUtil.copyProperties(user,UserDTO.class);
        //返回
        return Result.ok(userDTO);
    }

    //签到
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    //统计每月签到
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }

}
