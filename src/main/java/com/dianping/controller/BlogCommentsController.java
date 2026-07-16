package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.entity.Blog;
import com.dianping.entity.BlogComments;
import com.dianping.service.BlogCommentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

    @Autowired
    private BlogCommentsService blogCommentsService;

    /**
     * 发表评论
     * @param blogComments
     * @return
     */
    @PostMapping
    public Result saveComment(@RequestBody BlogComments blogComments){
        return blogCommentsService.saveComments(blogComments);
    }

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    @DeleteMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long commentId){
        return blogCommentsService.deleteComment(commentId);
    }

    /**
     * 根据博客id查询评论
     * @param blogId
     * @param current
     * @return
     */
    @GetMapping("/of/blog")
    public Result queryCommentByBlogId(
            @RequestParam("blogId") Long blogId,
            @RequestParam(value = "current",defaultValue = "1" ) Integer current
    ){
        return blogCommentsService.queryCommentsByBlogId(blogId,current);
    }

    /**
     * 查询某条一级评论的更多回复
     * @param parentId
     * @param current
     * @return
     */
    @GetMapping("/replies")
    public Result queryRepliesByParentId(
            @RequestParam("blogId") Long parentId,
            @RequestParam(value = "current",defaultValue = "1" ) Integer current
    ){
      return blogCommentsService.queryRepliesByParentId(parentId,current);
    }

    /**
     * 点赞/取消赞
     * @param commentId
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeComment(@PathVariable("id") Long commentId){
        return blogCommentsService.likeComment(commentId);
    }
}
