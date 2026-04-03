package com.quxiangshe.search.controller;

import com.quxiangshe.common.dto.Response;
import com.quxiangshe.search.dto.SearchResponseDTO;
import com.quxiangshe.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一搜索控制器
 * 支持笔记和用户的混合搜索
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 统一搜索接口
     * 支持搜索笔记和用户
     *
     * @param keyword 关键词
     * @param type    搜索类型：all-全部，note-仅笔记，user-仅用户
     * @param page    页码
     * @param size    每页数量
     * @return 搜索结果
     */
    @GetMapping({"/search", "/search/all"})
    public Response<SearchResponseDTO> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("统一搜索: keyword={}, type={}, page={}, size={}", keyword, type, page, size);
        return Response.success(searchService.search(keyword, type, page, size));
    }

    /**
     * 搜索笔记
     *
     * @param keyword  关键词
     * @param category 分类筛选
     * @param page     页码
     * @param size     每页数量
     * @return 笔记搜索结果
     */
    @GetMapping({"/search/notes", "/search/note"})
    public Response<SearchResponseDTO> searchNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("搜索笔记: keyword={}, category={}, page={}, size={}", keyword, category, page, size);
        return Response.success(searchService.searchNotes(keyword, category, page, size));
    }

    /**
     * 搜索用户
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页数量
     * @return 用户搜索结果
     */
    @GetMapping("/search/users")
    public Response<SearchResponseDTO> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("搜索用户: keyword={}, page={}, size={}", keyword, page, size);
        return Response.success(searchService.searchUsers(keyword, page, size));
    }

    /**
     * 创建搜索索引（管理员用）
     */
    @PostMapping("/admin/search/indexes")
    public Response<Void> createIndexes() {
        searchService.createIndexes();
        return Response.success();
    }
}
