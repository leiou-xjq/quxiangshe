package com.quxiangshe.note.service;

import com.quxiangshe.note.dto.NoteCreateRequestDTO;
import com.quxiangshe.note.dto.NoteResponseDTO;

/**
 * 笔记服务接口
 */
public interface NoteService {

    /**
     * 创建笔记
     *
     * @param userId 用户ID
     * @param request 创建请求
     * @return 笔记响应
     */
    NoteResponseDTO createNote(Long userId, NoteCreateRequestDTO request);

    /**
     * 获取笔记详情
     *
     * @param noteId 笔记ID
     * @param currentUserId 当前用户ID（可为空）
     * @return 笔记详情
     */
    NoteResponseDTO getNoteDetail(Long noteId, Long currentUserId);

    /**
     * 获取用户笔记列表
     *
     * @param userId 用户ID
     * @param lastId 游标ID
     * @param size 每页数量
     * @return 笔记列表
     */
    NoteListResponse getUserNotes(Long userId, Long lastId, Integer size);

    /**
     * 获取首页笔记列表
     *
     * @param lastId 游标ID
     * @param size 每页数量
     * @return 笔记列表
     */
    NoteListResponse getHomeNotes(Long lastId, Integer size);

    /**
     * 搜索笔记
     *
     * @param keyword 关键词
     * @param category 分类
     * @param page 页码
     * @param size 每页数量
     * @return 笔记列表
     */
    NoteListResponse searchNotes(String keyword, String category, Integer page, Integer size);

    /**
     * 点赞笔记
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void likeNote(Long userId, Long noteId);

    /**
     * 取消点赞
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void unlikeNote(Long userId, Long noteId);

    /**
     * 收藏笔记
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void collectNote(Long userId, Long noteId);

    /**
     * 取消收藏
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void uncollectNote(Long userId, Long noteId);

    /**
     * 删除笔记（逻辑删除）
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     */
    void deleteNote(Long userId, Long noteId);

    /**
     * 审核笔记（管理员操作）
     *
     * @param noteId 笔记ID
     * @param approved 是否通过
     */
    void reviewNote(Long noteId, boolean approved);

    /**
     * 获取笔记列表响应
     */
    class NoteListResponse {
        private java.util.List<NoteResponseDTO> items;
        private Long lastNoteId;
        private Boolean hasMore;

        public NoteListResponse(java.util.List<NoteResponseDTO> items, Long lastNoteId, Boolean hasMore) {
            this.items = items;
            this.lastNoteId = lastNoteId;
            this.hasMore = hasMore;
        }

        public java.util.List<NoteResponseDTO> getItems() { return items; }
        public Long getLastNoteId() { return lastNoteId; }
        public Boolean getHasMore() { return hasMore; }
    }
}
