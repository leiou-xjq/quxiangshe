package com.quxiangshe.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记实体
 * 对应数据库表 t_note
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_note")
public class NoteEntity {

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
     * 标题
     */
    private String title;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON数组）
     */
    private String tags;

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
    private Integer collectCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 状态：0=待审核，1=正常，2=违规，3=用户删除
     */
    private Integer status;

    /**
     * 审核状态：0=待审核，1=通过，2=拒绝
     */
    private Integer auditStatus;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 常量定义 ====================

    /**
     * 状态：待审核
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：正常
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 状态：违规
     */
    public static final int STATUS_VIOLATION = 2;

    /**
     * 状态：用户删除
     */
    public static final int STATUS_USER_DELETED = 3;

    /**
     * 审核状态：待审核
     */
    public static final int AUDIT_PENDING = 0;

    /**
     * 审核状态：通过
     */
    public static final int AUDIT_PASSED = 1;

    /**
     * 审核状态：拒绝
     */
    public static final int AUDIT_REJECTED = 2;

    /**
     * 逻辑删除：是
     */
    public static final int DELETED_YES = 1;

    /**
     * 逻辑删除：否
     */
    public static final int DELETED_NO = 0;
}
