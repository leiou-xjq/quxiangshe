package com.quxiangshe.backend.service;

import java.util.List;

/**
 * AI回复建议Service接口
 */
public interface IAiReplySuggestionService {

    /**
     * 为评论生成回复建议
     *
     * @param noteContent 笔记内容（用于上下文）
     * @param comment    评论内容
     * @return 建议回复列表（3-5条）
     */
    List<String> generateSuggestions(String noteContent, String comment);

    /**
     * 根据评论ID获取建议（包含缓存逻辑）
     *
     * @param noteId    笔记ID
     * @param commentId 评论ID
     * @param noteContent 笔记内容
     * @param comment   评论内容
     * @return 建议回复列表
     */
    List<String> getSuggestions(Long noteId, Long commentId, String noteContent, String comment);
}