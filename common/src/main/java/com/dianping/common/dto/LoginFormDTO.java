package com.dianping.common.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
    /** 注册角色：1 用户 / 2 商家 / 3 管理员，null 或非法默认普通用户 */
    private Integer role;
}
