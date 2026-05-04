package com.quxiangshe.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记VO - 用于列表展示
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "笔记VO")
public class NoteVO {
    
    @Schema(description = "笔记ID")
    private Long id;
    
    @Schema(description = "发布者用户ID")
    private Long userId;
    
    @Schema(description = "发布者昵称")
    private String nickname;
    
    @Schema(description = "发布者头像")
    private String avatar;
    
    @Schema(description = "笔记标题")
    private String title;
    
    @Schema(description = "笔记内容")
    private String content;
    
    @Schema(description = "图片列表")
    private List<String> images;
    
    @Schema(description = "视频URL")
    private String video;
    
    @Schema(description = "视频封面URL")
    private String videoCover;
    
    @Schema(description = "标签列表")
    private List<String> tags;
    
    @Schema(description = "地理位置")
    private String location;
    
    @Schema(description = "点赞数")
    private Integer likeCount;
    
    @Schema(description = "评论数")
    private Integer commentCount;
    
    @Schema(description = "收藏数")
    private Integer favoriteCount;
    
    @Schema(description = "浏览数")
    private Integer viewCount;
    
    @Schema(description = "转发数")
    private Integer forwardCount;
    
    @Schema(description = "是否已点赞")
    private Boolean liked;
    
    @Schema(description = "是否已收藏")
    private Boolean favorited;
    
    @Schema(description = "稳定随机排序字段")
    private BigDecimal stableRandom;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    
    @Schema(description = "状态: 0-待审核, 1-正常, 2-违规")
    private Integer status;
}
