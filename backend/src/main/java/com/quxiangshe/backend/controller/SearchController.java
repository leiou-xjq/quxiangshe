package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.service.ISearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索控制器
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "搜索", description = "搜索接口")
@Slf4j
@RestController
@RequestMapping("/search")
public class SearchController {
    
    public SearchController(ISearchService searchService) {
        this.searchService = searchService;
    }
    
    private final ISearchService searchService;
    
    /**
     * 搜索笔记
     */
    @Operation(summary = "搜索笔记")
    @GetMapping("/notes")
    public R<Map<String, Object>> searchNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<Object> searchAfter) {
        
        log.info("Controller接收keyword: {}, tags: {}, searchAfter: {}", keyword, tags, searchAfter);
        Map<String, Object> result = searchService.searchNotes(keyword, tags, size, searchAfter);
        return R.ok(result);
    }
    
    /**
     * 搜索用户
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/users")
    public R<Map<String, Object>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<Object> searchAfter) {
        
        log.info("Controller接收keyword: {}, searchAfter: {}", keyword, searchAfter);
        Map<String, Object> result = searchService.searchUsers(keyword, size, searchAfter);
        return R.ok(result);
    }
    
    /**
     * 创建索引
     */
    @Operation(summary = "创建索引")
    @PostMapping("/index")
    public R<?> createIndexes() {
        searchService.createIndexes();
        return R.ok("索引创建成功");
    }
    
    /**
     * 全量同步
     */
    @Operation(summary = "全量同步数据")
    @PostMapping("/sync")
    public R<Map<String, Long>> syncAll() {
        long noteCount = searchService.syncAllNotes();
        long userCount = searchService.syncAllUsers();
        return R.ok(Map.of("notes", noteCount, "users", userCount));
    }
    
    /**
     * 同步笔记
     */
    @Operation(summary = "同步笔记")
    @PostMapping("/sync/note/{noteId}")
    public R<?> syncNote(@PathVariable Long noteId) {
        searchService.syncNote(noteId);
        return R.ok("笔记同步成功");
    }
    
    /**
     * 同步用户
     */
    @Operation(summary = "同步用户")
    @PostMapping("/sync/user/{userId}")
    public R<?> syncUser(@PathVariable Long userId) {
        searchService.syncUser(userId);
        return R.ok("用户同步成功");
    }
    
    /**
     * 删除笔记
     */
    @Operation(summary = "删除笔记")
    @DeleteMapping("/note/{noteId}")
    public R<?> deleteNote(@PathVariable Long noteId) {
        searchService.deleteNote(noteId);
        return R.ok("笔记删除成功");
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户")
    @DeleteMapping("/user/{userId}")
    public R<?> deleteUser(@PathVariable Long userId) {
        searchService.deleteUser(userId);
        return R.ok("用户删除成功");
    }
}