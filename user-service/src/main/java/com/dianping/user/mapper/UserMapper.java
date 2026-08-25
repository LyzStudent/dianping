package com.dianping.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
