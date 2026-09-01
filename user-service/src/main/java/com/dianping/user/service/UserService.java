package com.dianping.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.common.dto.LoginFormDTO;
import com.dianping.common.dto.Result;
import com.dianping.user.entity.User;
import jakarta.servlet.http.HttpSession;

import jakarta.mail.MessagingException;

public interface UserService extends IService<User> {
    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    Result sendcode(String phone, HttpSession session) throws MessagingException;

    /**
     * 登录
     * @param loginForm
     * @param session
     * @return
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     * 密码登录
     * @param loginForm
     * @return
     */
    Result loginByPassword(LoginFormDTO loginForm);

    /**
     * 签到
     * @return
     */
    Result sign();

    /**
     * 统计签到次数
     * @return
     */
    Result signCount();

    /**
     * 登出
     * @param token
     * @return
     */
    Result logout(String token);

    /**
     * 注册
     * @param loginForm
     * @return
     */
    Result register(LoginFormDTO loginForm);

    /**
     * 手机号注册
     * @param loginForm
     * @return
     */
    Result registerByPhone(LoginFormDTO loginForm);

    Result signDate(String date);
}
