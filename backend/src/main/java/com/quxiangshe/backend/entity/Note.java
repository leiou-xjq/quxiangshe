package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 笔记实体类
 * 对应数据库表: note
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("note")
public class Note {
    
    /**
     * 笔记ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 发布者用户ID
     */
    private Long userId;
    
    /**
     * 笔记标题
     */
    private String title;
    
    /**
     * 笔记内容
     */
    private String content;
    
    /**
     * 图片JSON数组
     */
    private String images;
    
    /**
     * 视频URL
     */
    private String video;
    
    /**
     * 视频封面URL
     */
    private String videoCover;
    
    /**
     * 标签JSON数组
     */
    private String tags;
    
    /**
     * 地理位置
     */
    private String location;
    
    /**
     * 稳定随机排序字段（16位小数），用于发现精彩Feed流分页
     */
    private BigDecimal stableRandom;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 收藏数
     */
    private Integer favoriteCount;
    
    /**
     * 浏览数
     */
    private Integer viewCount;
    
    /**
     * 转发数
     */
    private Integer forwardCount;
    
    /**
     * 热度值（用于热门榜单排序）
     */
    private Double hotScore;
    
    /**
     * 状态: 0-待审核, 1-正常, 2-违规下架
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
