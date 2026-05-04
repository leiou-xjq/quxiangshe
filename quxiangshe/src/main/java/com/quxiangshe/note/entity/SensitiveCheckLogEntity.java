package com.quxiangshe.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感词校验日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_sensitive_check_log")
public class SensitiveCheckLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long userId;

    /**
     * 内容类型：0=标题，1=正文
     */
    private Integer contentType;

    private String originalContent;

    private String foundWords;

    /**
     * 校验结果：0=通过，1=敏感词替换，2=拒绝
     */
    private Integer checkResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final int CONTENT_TYPE_TITLE = 0;
    public static final int CONTENT_TYPE_CONTENT = 1;

    public static final int CHECK_RESULT_PASSED = 0;
    public static final int CHECK_RESULT_REPLACED = 1;
    public static final int CHECK_RESULT_REJECTED = 2;
}
