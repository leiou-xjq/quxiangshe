package com.quxiangshe.backend.service;

import com.quxiangshe.backend.dto.CreateNoteRequest;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.vo.NoteVO;

import java.util.List;

/**
 * 笔记服务接口
 * 
 * @author 趣享社技术团队
 */
public interface INoteService {
    
    /**
     * 发布笔记
     * @param userId 用户ID
     * @param request 发布请求
     * @return 笔记VO
     */
    NoteVO createNote(Long userId, CreateNoteRequest request);
    
    /**
     * 获取笔记列表
     * @param page 页码
     * @param size 每页数量
     * @param userId 当前用户ID（用于判断点赞收藏状态）
     * @return 笔记列表
     */
    List<NoteVO> getNoteList(int page, int size, Long userId);
    
    /**
     * 获取笔记详情
     * @param noteId 笔记ID
     * @param userId 当前用户ID
     * @return 笔记VO
     */
    NoteVO getNoteDetail(Long noteId, Long userId);
    
    /**
     * 删除笔记
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteNote(Long noteId, Long userId);
    
    /**
     * 点赞笔记
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean likeNote(Long noteId, Long userId);
    
    /**
     * 取消点赞
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unlikeNote(Long noteId, Long userId);
    
    /**
     * 收藏笔记
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean favoriteNote(Long noteId, Long userId);
    
    /**
     * 取消收藏
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unfavoriteNote(Long noteId, Long userId);
    
    /**
     * 获取当前用户的笔记列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID
     * @return 笔记列表
     */
    List<NoteVO> getMyNotes(Long userId, int page, int size, Long currentUserId);
    
    /**
     * 获取当前用户的收藏列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID
     * @return 笔记列表
     */
    List<NoteVO> getMyFavorites(Long userId, int page, int size, Long currentUserId);
    
    /**
     * 获取当前用户的获赞数
     * @param userId 用户ID
     * @return 获赞数
     */
    long getMyLikesCount(Long userId);
    
    /**
     * 发现精彩 - 稳定随机排序展示笔记（游标分页）
     * 使用 stable_random 字段实现稳定排序，避免翻页重复
     * @param cursor 游标（stable_random_noteId），首次请求为空
     * @param size 每页数量
     * @param userId 当前用户ID（用于过滤已浏览笔记）
     * @return 笔记列表
     */
    List<NoteVO> getDiscoverNotes(String cursor, int size, Long userId);
    
    /**
     * 热门 - 热度排序笔记（游标分页）
     * @param cursor 游标（时间戳_noteId）
     * @param size 每页数量
     * @param userId 当前用户ID
     * @return 笔记列表
     */
    List<NoteVO> getPopularNotes(String cursor, int size, Long userId);
    
    /**
     * 刷新热门笔记热度（带全局限流）
     * 访问热门Tab时调用
     */
    void refreshHotScoreIfNeeded();
    
    /**
     * 转发笔记
     * @param noteId 原笔记ID
     * @param userId 转发者用户ID
     * @param content 转发时的评论内容
     * @return 是否成功
     */
    boolean forwardNote(Long noteId, Long userId, String content);
    
    /**
     * 增量更新笔记热度
     * @param noteId 笔记ID
     * @param increment 热度增量
     */
    void incrementHotScore(Long noteId, int increment);
    
    /**
     * 根据ID获取笔记
     * @param noteId 笔记ID
     * @return 笔记实体
     */
    Note getNoteById(Long noteId);
}
