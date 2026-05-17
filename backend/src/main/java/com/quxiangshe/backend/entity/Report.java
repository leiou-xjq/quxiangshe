package com.quxiangshe.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报记录实体类，对应数据库表 report。
 * <p>
 * 记录用户对违规内容的举报信息，支持三种举报目标：
 * <ul>
 *   <li>targetType=1：举报笔记</li>
 *   <li>targetType=2：举报评论</li>
 *   <li>targetType=3：举报用户</li>
 * </ul>
 * 举报状态流转：0-待处理 → 1-已处理 / 2-已驳回。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Data
@TableName("report")
public class Report {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 举报者ID
     */
    @TableField("reporter_id")
    private Long reporterId;
    
    /**
     * 目标类型: 1-笔记 2-评论 3-用户
     */
    @TableField("target_type")
    private Integer targetType;
    
    /**
     * 被举报的目标ID
     */
    @TableField("target_id")
    private Long targetId;
    
    /**
     * 举报原因: 1-垃圾广告 2-涉黄 3-抄袭 4-其他
     */
    @TableField("reason")
    private Integer reason;
    
    /**
     * 详细描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 状态: 0-待处理 1-已处理 2-已驳回
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;
}