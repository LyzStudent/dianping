package com.dianping.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.service.BlogService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Autowired
    private BlogService blogService;

    /**
     * 保存博客
     * @param blog
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog){
        return blogService.saveBlog(blog);
    }

    /**
     * 点赞
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id){
        return blogService.likeBlog(id);
    }

    /**
     * 查询点赞数
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable Integer id){
        return blogService.queryBlogLikes(id);
    }

    /**
     * 查询当前用户所有博客
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current",defaultValue = "1") Integer current){
        //获取当前用户
        UserDTO user = UserHolder.getUser();

        //根据用户查询
        Page<Blog> page=blogService.query()
                .eq("user_id",user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        //获取当前页数据
        List<Blog> records=page.getRecords();
        return Result.ok(records);
    }


    /**
     * 查询某个用户所有博客
     * @param current
     * @param id
     * @return
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current",defaultValue = "1") Integer current,
            @RequestParam("id") Long id){
        //根据用户查询
        Page<Blog> page=blogService.query()
                .eq("user_id",id).page(new Page<>(current,SystemConstants.MAX_PAGE_SIZE));

        //获取当前页数据
        List<Blog> records=page.getRecords();
        return Result.ok(records);
    }


    /**
     * 滚动分页查询关注博客动态
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,@RequestParam(value = "offset",defaultValue = "0") Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }


    /**
     * 查询热门博客
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current",defaultValue = "1") Integer current ){
        return blogService.queryHotBlog(current);
    }

    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable Integer id){
        return blogService.queryById(id);
    }



}
