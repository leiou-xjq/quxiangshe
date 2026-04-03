package com.quxiangshe.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记点赞实体
 * 对应数据库表 t_note_like
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_note_like")
public class NoteLikeEntity {

    /**
     * 点赞记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 点赞用户ID
     */
    private Long userId;

    /**
     * 点赞时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
