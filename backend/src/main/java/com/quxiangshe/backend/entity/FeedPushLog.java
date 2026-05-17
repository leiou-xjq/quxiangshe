package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Feed推送日志实体类，对应数据库表 feed_push_log。
 * <p>
 * 记录每次Feed推送的投递状态，用于追踪笔记从作者到目标用户的推送情况。
 * 支持三种推送模式：
 * <ul>
 *   <li>推模式（PUSH_MODE_PUSH=1）：笔记推送至粉丝收件箱</li>
 *   <li>拉模式（PUSH_MODE_PULL=2）：粉丝从发件箱拉取</li>
 *   <li>推拉结合（PUSH_MODE_HYBRID=3）：活跃粉丝推、非活跃粉丝拉</li>
 * </ul>
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("feed_push_log")
public class FeedPushLog {
    
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 笔记ID
     */
    @TableField("note_id")
    private Long noteId;
    
    /**
     * 作者ID
     */
    @TableField("author_id")
    private Long authorId;
    
    /**
     * 接收推送的用户ID
     */
    @TableField("target_user_id")
    private Long targetUserId;
    
    /**
     * 推送模式: 1-推模式(收件箱), 2-拉模式(发件箱), 3-推拉结合(收件箱)
     */
    @TableField("push_mode")
    private Integer pushMode;
    
    /**
     * 推送状态: 0-失败, 1-成功
     */
    @TableField("push_status")
    private Integer pushStatus;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    public static final int PUSH_MODE_PUSH = 1;
    public static final int PUSH_MODE_PULL = 2;
    public static final int PUSH_MODE_HYBRID = 3;
    
    public static final int PUSH_STATUS_FAIL = 0;
    public static final int PUSH_STATUS_SUCCESS = 1;
}