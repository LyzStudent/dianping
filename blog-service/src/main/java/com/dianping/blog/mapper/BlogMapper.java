package com.dianping.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dianping.blog.entity.Blog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
}
