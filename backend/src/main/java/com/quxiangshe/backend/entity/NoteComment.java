package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记评论实体类
 * 对应数据库表: note_comment
 * 
 * 抖音风格评论结构：
 * - parentId = 0: 根评论（一级评论）
 * - parentId > 0: 子评论（回复）
 * - rootId: 所属根评论ID（用于前端快速定位）
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note_comment")
public class NoteComment {
    
    /**
     * 评论ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 笔记ID
     */
    private Long noteId;
    
    /**
     * 评论者用户ID
     */
    private Long userId;
    
    /**
     * 父评论ID(用于回复)
     * = 0 表示根评论
     * > 0 表示回复某条评论
     */
    private Long parentId;
    
    /**
     * 所属根评论ID（用于前端渲染）
     * = 0 表示根评论本身
     * > 0 表示该评论属于哪个根评论
     */
    private Long rootId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 回复数量
     */
    private Integer replyCount;
    
    /**
     * 热度分数（用于排序）
     */
    private Double hotScore;
    
    /**
     * 评论者昵称（非数据库字段）
     */
    @TableField(exist = false)
    private String nickname;
    
    /**
     * 评论者头像（非数据库字段）
     */
    @TableField(exist = false)
    private String avatar;
    
    /**
     * 被回复者昵称（非数据库字段，用于显示"回复 @XXX"）
     */
    @TableField(exist = false)
    private String replyToNickname;
    
    /**
     * 状态: 0-待审核, 1-正常, 2-违规删除
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 删除时间(软删除)
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}
