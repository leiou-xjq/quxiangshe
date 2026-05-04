package com.quxiangshe.search.service;

import com.quxiangshe.search.dto.SearchResponseDTO;

/**
 * 统一搜索服务接口
 * 支持笔记和用户的混合搜索
 */
public interface SearchService {

    /**
     * 统一搜索
     *
     * @param keyword 关键词
     * @param type    搜索类型：all-全部，note-仅笔记，user-仅用户
     * @param page    页码
     * @param size    每页数量
     * @return 搜索结果
     */
    SearchResponseDTO search(String keyword, String type, Integer page, Integer size);

    /**
     * 搜索笔记
     *
     * @param keyword  关键词
     * @param category 分类
     * @param page     页码
     * @param size     每页数量
     * @return 笔记搜索结果
     */
    SearchResponseDTO searchNotes(String keyword, String category, Integer page, Integer size);

    /**
     * 搜索用户
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页数量
     * @return 用户搜索结果
     */
    SearchResponseDTO searchUsers(String keyword, Integer page, Integer size);

    /**
     * 索引用户文档
     *
     * @param userId 用户ID
     */
    void indexUser(Long userId);

    /**
     * 删除用户索引
     *
     * @param userId 用户ID
     */
    void deleteUserIndex(Long userId);

    /**
     * 创建搜索索引
     */
    void createIndexes();
}
