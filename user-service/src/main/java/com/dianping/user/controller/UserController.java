package com.dianping.user.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.annotation.RequireRole;
import com.dianping.common.dto.LoginFormDTO;
import com.dianping.common.dto.Result;
import com.dianping.common.dto.UserDTO;
import com.dianping.common.util.UserHolder;
import com.dianping.user.entity.User;
import com.dianping.user.entity.UserInfo;
import com.dianping.user.service.UserInfoService;
import com.dianping.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


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
     * 手机号注册
     * @param loginForm
     * @return
     */
    @PostMapping("/registerByPhone")
    public Result registerByPhone(@RequestBody LoginFormDTO loginForm){
        return userService.registerByPhone(loginForm);
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
    @GetMapping("/me")
    public Result me(){
        //获取当前登录用户并返回
        UserDTO user= UserHolder.getUser();
        User db=userService.getById(user.getId());
        if(db!=null){
            user.setPoints(db.getPoints());
            user.setLevel(userService.calcLevel(db.getPoints()));
        }
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
    @GetMapping("/{id}")
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

    @GetMapping("/sign/date")
    public Result signDate(@RequestParam(value = "date",required = false) String date){
        return userService.signDate(date);
    }

    //单个用户
    @GetMapping("/dto/{id}")
    public UserDTO queryDTOById(@PathVariable("id") Long userId){
        User user=userService.getById(userId);
        return user==null?null:BeanUtil.copyProperties(user,UserDTO.class);
    }

    //批量，保证按传入ids顺序返回
    @GetMapping("/dtos")
    public List<UserDTO> queryDtosByIds(@RequestParam("ids") String ids){
        List<Long> idList= Arrays.stream(ids.split(",")).map(Long::valueOf).toList();
        List<User> users=userService.listByIds(idList);

        Map<Long,User> map=users.stream().collect(Collectors.toMap(User::getId,u->u));
        return idList.stream().map(map::get).filter(Objects::nonNull)
                .map(u->BeanUtil.copyProperties(u,UserDTO.class)).toList();
    }

    /**
     * 内部Feign接口（seata分布式事务演示：加积分）
     * @param userId
     * @param points
     * @return
     */
    @PostMapping("/inner/points/add")
    public Result addPoints(@RequestParam("userId") Long userId,
                            @RequestParam("points") Integer points){
        boolean ok=userService.update()
                .setSql("points=points+ "+points)
                .eq("id",userId)
                .update();

        return ok?Result.ok():Result.fail("加入积分失败");
    }

    /**
     * 设置密码
     */
    @PostMapping("/register")
    public Result register(@RequestBody LoginFormDTO loginForm){
        return userService.register(loginForm);
    }

    @PostMapping("/loginByPassword")
    public Result loginByPassword(@RequestBody LoginFormDTO loginForm){
        return userService.loginByPassword(loginForm);
    }

    @GetMapping("/admin/user/list")
    @RequireRole("3")
    public Result adminUserList(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size){
        return Result.ok(userService.page(new Page<>(page,size)).getRecords());
    }

    /**
     * 封禁用户：写redis黑名单把该用户所有登录态拉黑
     * 直接改用户role=0，登陆时<=0拒绝
     * @param userId
     * @return
     */
    @PostMapping("/admin/user/{id}/ban")
    @RequireRole("3")
    public Result adminBanUser(@PathVariable("id") Long userId){
        boolean ok=userService.update().setSql("role=0").eq("id",userId).update();
        return ok?Result.ok():Result.fail("操作失败");
    }

    /**
     * 解封用户:恢复为普通用户(role=1),登录限制解除
     * @return
     */
    @GetMapping("/admin/stats/user-count")
    @RequireRole("3")
    public Result userCount(){
        return Result.ok(userService.count());
    }

    @PostMapping("/admin/user/{id}/unban")
    public Result adminUnbanUser(@PathVariable("id") Long userId){
        boolean ok=userService.update().setSql("role=1").eq("id",userId).update();
        return ok?Result.ok():Result.fail("操作失败");

    }

}
