package com.quxiangshe.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评论排序数据（Redis存储用，非数据库实体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentSortData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 评论ID */
    private Long commentId;
    
    /** 笔记ID */
    private Long postId;
    
    /** 根评论ID（0表示根评论） */
    private Long rootId;
    
    /** 父评论ID（0表示直接回复笔记） */
    private Long parentId;
    
    /** 用户ID */
    private Long userId;
    
    /** 昵称 */
    private String nickname;
    
    /** 头像 */
    private String avatar;
    
    /** 评论内容 */
    private String content;
    
    /** 点赞数 */
    private Integer likeCount;
    
    /** 回复数 */
    private Integer replyCount;
    
    /** 创建时间戳（毫秒） */
    private Long createdAt;
    
    /** 状态：1-正常 */
    private Integer status;
    
    /** 被回复者昵称 */
    private String replyToNickname;
}