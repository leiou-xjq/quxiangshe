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
 * 搜索服务实现类 - 基于MySQL LIKE查询
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {
    
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    
    @Override
    public void createIndexes() {
        log.info("使用MySQL数据库，无需创建索引");
    }
    
    @Override
    public Map<String, Object> searchNotes(String keyword, List<String> tags, int size, List<Object> searchAfter) {
        try {
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
    
    @Override
    public void syncNote(Long noteId) {
        log.info("使用MySQL数据库，无需同步到ES: noteId={}", noteId);
    }
    
    @Override
    public void syncUser(Long userId) {
        log.info("使用MySQL数据库，无需同步到ES: userId={}", userId);
    }
    
    @Override
    public void deleteNote(Long noteId) {
        log.info("使用MySQL数据库，无需从ES删除: noteId={}", noteId);
    }
    
    @Override
    public void deleteUser(Long userId) {
        log.info("使用MySQL数据库，无需从ES删除: userId={}", userId);
    }
    
    @Override
    public long syncAllNotes() {
        log.info("使用MySQL数据库，无需全量同步到ES");
        return 0;
    }
    
    @Override
    public long syncAllUsers() {
        log.info("使用MySQL数据库，无需全量同步到ES");
        return 0;
    }
}