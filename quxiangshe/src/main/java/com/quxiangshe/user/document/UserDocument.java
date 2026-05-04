package com.quxiangshe.user.document;

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

/**
 * 用户ES文档
 * 对应Elasticsearch索引 t_user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "t_user")
@Setting(settingPath = "elasticsearch/settings.json")
public class UserDocument {

    /**
     * 用户ID
     */
    @Id
    private Long id;

    /**
     * 用户名（支持分词搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String username;

    /**
     * 昵称（支持分词搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String nickname;

    /**
     * 手机号（精确匹配）
     */
    @Field(type = FieldType.Keyword)
    private String phone;

    /**
     * 头像URL
     */
    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    /**
     * 个人简介
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String bio;

    /**
     * 状态：0禁用1正常2待审核
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 最后登录时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
}
