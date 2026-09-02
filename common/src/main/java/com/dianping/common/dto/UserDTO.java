package com.dianping.common.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    private Integer role;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 会员等级:普卡 / 银卡 / 金卡 / 黑金
     */
    private String level;
}
