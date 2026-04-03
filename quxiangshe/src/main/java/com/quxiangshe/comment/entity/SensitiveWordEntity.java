package com.quxiangshe.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感词实体
 * 用于存储敏感词库
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_sensitive_word")
public class SensitiveWordEntity {

    /**
     * 敏感词ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词
     */
    private String word;

    /**
     * 分类
     */
    private String category;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
