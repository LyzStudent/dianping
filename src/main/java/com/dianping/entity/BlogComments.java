package com.dianping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_blog_comments")
public class BlogComments implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 探店id
     */
    @TableField("blog_id")
    private Long blogId;

    /**
     * 关联的1级评论id，如果是1级评论，则值为0
     */
    private Long parentId;

    /**
     * 回复的评论
     */
    private Long answerId;

    /**
     * 回复的内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer liked;

    /**
     * 状态，0：正常 1：被举报 2:禁止查看
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    //以下是非数据库字段，用于前端展示
    /**
     * 评论者昵称
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 评论者头像
     */
    @TableField(exist = false)
    private String icon;

    /**
     * 回复的评论昵称
     */
    @TableField(exist = false)
    private String answerNickName;

    /**
     * 当前用户是否已点赞
     */
    @TableField(exist = false)
    private Boolean isLike;

    /**
     * 一级评论下的二级评论
     */
    @TableField(exist = false)
    private List<BlogComments> replies;
}
