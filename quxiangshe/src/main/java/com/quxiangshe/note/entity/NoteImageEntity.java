package com.quxiangshe.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记图片实体
 * 对应数据库表 t_note_image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_note_image")
public class NoteImageEntity {

    /**
     * 图片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 图片顺序
     */
    private Integer imageOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
