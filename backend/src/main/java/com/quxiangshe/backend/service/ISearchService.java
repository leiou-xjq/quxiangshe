package com.quxiangshe.backend.service;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务接口（基于 MySQL 搜索）
 *
 * @author 理享技术团队
 */
public interface ISearchService {
    
    /**
     * 搜索笔记 (模糊搜索 + 热度排序 + 游标分页)
     * @param keyword 搜索关键词
     * @param tags 标签列表筛选
     * @param size 每页数量
     * @param searchAfter 游标 (格式: [hotScore, createdAt, id])
     * @return 搜索结果 (含data和nextSearchAfter)
     */
    Map<String, Object> searchNotes(String keyword, List<String> tags, int size, List<Object> searchAfter);
    
    /**
     * 搜索用户 (模糊搜索 + 游标分页)
     * @param keyword 搜索关键词
     * @param size 每页数量
     * @param searchAfter 游标 (格式: [id])
     * @return 搜索结果 (含data和nextSearchAfter)
     */
    Map<String, Object> searchUsers(String keyword, int size, List<Object> searchAfter);
    
    /**
     * 同步笔记到ES
     * @param noteId 笔记ID
     */
    void syncNote(Long noteId);
    
    /**
     * 同步用户到ES
     * @param userId 用户ID
     */
    void syncUser(Long userId);
    
    /**
     * 从ES删除笔记
     * @param noteId 笔记ID
     */
    void deleteNote(Long noteId);
    
    /**
     * 从ES删除用户
     * @param userId 用户ID
     */
    void deleteUser(Long userId);
    
    /**
     * 全量同步笔记到ES
     * @return 同步数量
     */
    long syncAllNotes();
    
    /**
     * 全量同步用户到ES
     * @return 同步数量
     */
    long syncAllUsers();
    
    /**
     * 创建ES索引
     */
    void createIndexes();
}