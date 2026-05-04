package com.quxiangshe.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记收藏实体
 * 对应数据库表 t_note_collect
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_note_collect")
public class NoteCollectEntity {

    /**
     * 收藏记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 收藏用户ID
     */
    private Long userId;

    /**
     * 收藏时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
