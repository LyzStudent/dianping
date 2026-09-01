package com.dianping.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.shop.entity.Shop;
import com.dianping.shop.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
