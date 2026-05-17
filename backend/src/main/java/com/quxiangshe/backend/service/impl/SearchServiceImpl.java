package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 搜索服务实现类
 *
 * <p>当前基于 MySQL LIKE 查询实现搜索功能。
 * 预留了 Elasticsearch 同步接口（createIndexes、syncNote、syncUser 等），
 * 在 MySQL 模式下这些接口均为空实现，方便后续平滑迁移至 ES。
 *
 * <p>所属业务模块：内容搜索管理
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {
    
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    
    /**
     * 创建搜索索引
     *
     * <p>MySQL 模式下无需创建索引，ES 模式下需实现索引初始化逻辑。
     */
    @Override
    public void createIndexes() {
        log.info("使用MySQL数据库，无需创建索引");
    }
    
    /**
     * 搜索笔记
     *
     * <p>使用基于 offset 的分页，"searchAfter" 参数被复用为 offset 值。
     * tags 参数会被传递到 MyBatis Mapper 的 XML 动态 SQL 中进行标签匹配。
     *
     * @param keyword     搜索关键词
     * @param tags        标签过滤（可选）
     * @param size        每页大小
     * @param searchAfter ES 风格的分页游标（本实现中复用为 offset）
     * @return 搜索结果 Map，包含 data、hasMore、nextSearchAfter 三个 key
     */
    @Override
    public Map<String, Object> searchNotes(String keyword, List<String> tags, int size, List<Object> searchAfter) {
        try {
            // 将 searchAfter 转换为 offset 用于 MySQL 分页
            int offset = 0;
            if (searchAfter != null && !searchAfter.isEmpty()) {
                try {
                    offset = Integer.parseInt(searchAfter.get(0).toString());
                } catch (Exception e) {
                    offset = 0;
                }
            }
            
            log.info("搜索笔记: keyword={}, tags={}, size={}, offset={}", keyword, tags, size, offset);
            List<Note> notes = noteMapper.searchNotes(keyword, tags, size, offset);
            log.info("搜索结果: count={}, hasMore={}", notes.size(), notes.size() == size);
            
            int nextOffset = offset + size;
            Map<String, Object> result = new HashMap<>();
            result.put("data", notes);
            result.put("hasMore", notes.size() == size);
            result.put("nextSearchAfter", List.of(nextOffset));
            return result;
        } catch (Exception e) {
            log.error("搜索笔记失败: keyword={}", keyword, e);
            throw new RuntimeException("搜索笔记失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索用户
     *
     * @param keyword     搜索关键词（匹配昵称）
     * @param size        每页大小
     * @param searchAfter 分页游标（本实现中复用为 offset）
     * @return 搜索结果 Map
     */
    @Override
    public Map<String, Object> searchUsers(String keyword, int size, List<Object> searchAfter) {
        try {
            int offset = 0;
            if (searchAfter != null && !searchAfter.isEmpty()) {
                try {
                    offset = Integer.parseInt(searchAfter.get(0).toString());
                } catch (Exception e) {
                    offset = 0;
                }
            }
            
            List<User> users = userMapper.searchUsers(keyword, size, offset);
            
            int nextOffset = offset + size;
            Map<String, Object> result = new HashMap<>();
            result.put("data", users);
            result.put("hasMore", users.size() == size);
            result.put("nextSearchAfter", List.of(nextOffset));
            return result;
        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}", keyword, e);
            throw new RuntimeException("搜索用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步单篇笔记到搜索引擎
     *
     * <p>MySQL 模式下为空实现。
     *
     * @param noteId 笔记ID
     */
    @Override
    public void syncNote(Long noteId) {
        log.info("使用MySQL数据库，无需同步到ES: noteId={}", noteId);
    }
    
    /**
     * 同步单个用户到搜索引擎
     *
     * <p>MySQL 模式下为空实现。
     *
     * @param userId 用户ID
     */
    @Override
    public void syncUser(Long userId) {
        log.info("使用MySQL数据库，无需同步到ES: userId={}", userId);
    }
    
    /**
     * 从搜索引擎中删除笔记索引
     *
     * <p>MySQL 模式下为空实现。
     *
     * @param noteId 笔记ID
     */
    @Override
    public void deleteNote(Long noteId) {
        log.info("使用MySQL数据库，无需从ES删除: noteId={}", noteId);
    }
    
    /**
     * 从搜索引擎中删除用户索引
     *
     * <p>MySQL 模式下为空实现。
     *
     * @param userId 用户ID
     */
    @Override
    public void deleteUser(Long userId) {
        log.info("使用MySQL数据库，无需从ES删除: userId={}", userId);
    }
    
    /**
     * 全量同步所有笔记到搜索引擎
     *
     * <p>MySQL 模式下为空实现，返回 0。
     *
     * @return 同步数量（MySQL 模式下永远返回 0）
     */
    @Override
    public long syncAllNotes() {
        log.info("使用MySQL数据库，无需全量同步到ES");
        return 0;
    }
    
    /**
     * 全量同步所有用户到搜索引擎
     *
     * <p>MySQL 模式下为空实现，返回 0。
     *
     * @return 同步数量（MySQL 模式下永远返回 0）
     */
    @Override
    public long syncAllUsers() {
        log.info("使用MySQL数据库，无需全量同步到ES");
        return 0;
    }
}