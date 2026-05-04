package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户活跃度实体类
 * 对应数据库表: user_activity
 * 记录用户登录和互动数据，用于推拉结合模式判断活跃粉丝
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("user_activity")
public class UserActivity {
    
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 累计登录天数
     */
    @TableField("login_days")
    private Integer loginDays;
    
    /**
     * 累计互动次数
     */
    @TableField("interaction_count")
    private Integer interactionCount;
    
    /**
     * 活跃分数
     * 计算公式: 5(基础分) + loginDays * 5 + min(interactionCount * 3, 200)
     */
    @TableField("activity_score")
    private Double activityScore;
    
    /**
     * 上次登录日期
     */
    @TableField("last_login_date")
    private LocalDate lastLoginDate;
    
    /**
     * 今日互动次数
     */
    @TableField("today_interaction_count")
    private Integer todayInteractionCount;
    
    /**
     * 今日互动日期
     */
    @TableField("today_interaction_date")
    private LocalDate todayInteractionDate;
    
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
     * 基础分
     */
    public static final double BASE_SCORE = 5.0;
    
    /**
     * 登录权重
     */
    public static final double LOGIN_WEIGHT = 5.0;
    
    /**
     * 互动权重
     */
    public static final double INTERACTION_WEIGHT = 3.0;
    
    /**
     * 每日互动得分上限
     */
    public static final double MAX_DAILY_INTERACTION_SCORE = 200.0;
    
    /**
     * 活跃判定阈值
     */
    public static final double ACTIVITY_THRESHOLD = 50.0;
    
    /**
     * 计算活跃分数
     */
    public void calculateScore() {
        double interactionScore = Math.min(this.interactionCount * INTERACTION_WEIGHT, MAX_DAILY_INTERACTION_SCORE);
        this.activityScore = BASE_SCORE + this.loginDays * LOGIN_WEIGHT + interactionScore;
    }
}