package com.quxiangshe.note.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记ES文档
 * 对应Elasticsearch索引 t_note
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "t_note")
@Setting(settingPath = "elasticsearch/settings.json")
public class NoteDocument {

    /**
     * 笔记ID
     */
    @Id
    private Long id;

    /**
     * 发布者用户ID
     */
    @Field(type = FieldType.Long)
    private Long userId;

    /**
     * 发布者昵称
     */
    @Field(type = FieldType.Text)
    private String nickname;

    /**
     * 发布者用户名
     */
    @Field(type = FieldType.Text)
    private String username;

    /**
     * 发布者头像
     */
    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    /**
     * 标题（支持分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 正文内容（支持分词）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 封面图片URL
     */
    @Field(type = FieldType.Keyword)
    private String coverImage;

    /**
     * 分类（精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 标签（精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 点赞数
     */
    @Field(type = FieldType.Integer)
    private Integer likeCount;

    /**
     * 评论数
     */
    @Field(type = FieldType.Integer)
    private Integer commentCount;

    /**
     * 收藏数
     */
    @Field(type = FieldType.Integer)
    private Integer collectCount;

    /**
     * 浏览数
     */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /**
     * 状态：0=待审核，1=正常，2=违规，3=用户删除
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 逻辑删除：0=正常，1=已删除
     */
    @Field(type = FieldType.Integer)
    private Integer deleted;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}
