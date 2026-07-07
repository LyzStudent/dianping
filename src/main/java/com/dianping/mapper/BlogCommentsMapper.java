package com.dianping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.entity.BlogComments;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogCommentsMapper extends BaseMapper<BlogComments> {
}
