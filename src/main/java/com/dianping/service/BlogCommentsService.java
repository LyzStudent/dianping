package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.Result;
import com.dianping.entity.BlogComments;

public interface BlogCommentsService extends IService<BlogComments> {

    /**
     * 保存评论
     * @param blogComments
     * @return
     */
    Result saveComments(BlogComments blogComments);

    /**
     * 删除评论
     * @param commentId
     * @return
     */
    Result deleteComment(Long commentId);

    /**
     * 根据博客id查询评论
     * @param blogId
     * @param current
     * @return
     */
    Result queryCommentsByBlogId(Long blogId, Integer current);

    /**
     * 查询某条一级评论的更多回复
     * @param parentId
     * @param current
     * @return
     */
    Result queryRepliesByParentId(Long parentId, Integer current);

    /**
     *点赞/取消赞评论
     * @param commentId
     * @return
     */
    Result likeComment(Long commentId);
}
