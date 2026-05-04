package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞实体
 */
@Data
@TableName("comment_like")
public class CommentLike {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long commentId;
    
    private Long userId;
    
    private LocalDateTime createdAt;
}