package com.quxiangshe.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentTreeVO {
    
    private Long commentId;
    private Long postId;
    private Long parentId;
    private Long rootId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Long createdAt;
    private Integer status;
    private String replyToNickname;
    private Double hotScore;
    private Boolean hasMoreChildren;
    private Integer totalChildren;
    
    @Builder.Default
    private List<CommentTreeVO> children = new ArrayList<>();
    
    public static final int STATUS_NORMAL = 1;
}