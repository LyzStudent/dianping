package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Follow;

public interface FollowService extends IService<Follow> {

    /**
     * 判断当前用户是否关注了某个用户
     * @param followUserId
     * @return
     */
    Result isFollow(Long followUserId);

    /**
     * 关注或者取消关注
     * @param followUserId
     * @param isFellow
     * @return
     */
    Result follow(Long followUserId, Boolean isFellow);


    /**
     * 查询共同关注
     * @param id
     * @return
     */
    Result followCommons(Long id);
}
