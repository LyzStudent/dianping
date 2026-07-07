package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.Blog;

public interface BlogService extends IService<Blog> {

    /**
     * 保存博文
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * 点赞
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 查询点赞
     * @param id
     * @return
     */
    Result queryBlogLikes(Integer id);

    /**
     * 滚动分页查询关注博客动态
     * @param max
     * @param offset
     * @return
     */
    Result queryBlogOfFollow(Long max, Integer offset);


    /**
     * 查询热门博客
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    Result queryById(Integer id);
}
