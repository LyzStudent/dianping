package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.entity.User;
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
}
