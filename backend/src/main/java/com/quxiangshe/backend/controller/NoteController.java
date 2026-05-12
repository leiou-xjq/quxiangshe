package com.quxiangshe.backend.controller;

import com.quxiangshe.backend.annotation.RateLimit;
import com.quxiangshe.backend.common.R;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.CreateCommentRequest;
import com.quxiangshe.backend.dto.CreateNoteRequest;
import com.quxiangshe.backend.dto.FeedPushMessage;
import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.entity.CommentTreeResponse;
import com.quxiangshe.backend.entity.CommentTreeVO;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.entity.NoteComment;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.service.ICommentService;
import com.quxiangshe.backend.service.ICommentSortService;
import com.quxiangshe.backend.service.IFollowService;
import com.quxiangshe.backend.service.IAiReplySuggestionService;
import com.quxiangshe.backend.service.INoteService;
import com.quxiangshe.backend.service.IOssService;
import com.quxiangshe.backend.service.IUserService;
import com.quxiangshe.backend.service.sort.FullSortStrategy;
import com.quxiangshe.backend.vo.NoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneId;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;;

/**
 * 笔记控制器
 * 提供笔记发布、查询、点赞、收藏、评论等接口
 * 
 * @author 趣享社技术团队
 */
@Tag(name = "笔记管理", description = "笔记相关接口")
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@Slf4j
public class NoteController {
    
    private final INoteService noteService;
    private final ICommentService commentService;
    private final ICommentSortService commentSortService;
    private final FullSortStrategy fullSortStrategy;
    private final IFollowService followService;
    private final IAiReplySuggestionService aiReplySuggestionService;
    private final IOssService ossService;
    private final IUserService userService;
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getAttribute("userId") != null ? 
            request.getAttribute("userId").toString() : null;
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    
    /**
     * 发布笔记
     * 敏感词检测由AOP统一处理
     * 发布后异步推送到粉丝Feed流
     */
    @Operation(summary = "发布笔记")
    @PostMapping("/create")
    public R<NoteVO> createNote(
            @Valid @RequestBody CreateNoteRequest requestBody,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        NoteVO note = noteService.createNote(userId, requestBody);
        
        return R.ok("发布成功", note);
    }
    
    /**
     * 获取笔记列表
     */
    @Operation(summary = "获取笔记列表")
    @GetMapping("/list")
    public R<List<NoteVO>> getNoteList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<NoteVO> list = noteService.getNoteList(page, size, userId);
        return R.ok(list);
    }
    
    /**
     * 发现精彩 - 动态随机展示笔记（每次刷新都不同）
     * cursor 格式：已展示的笔记ID列表（逗号分隔）
     */
    @Operation(summary = "发现精彩")
    @GetMapping("/discover")
    public R<Map<String, Object>> getDiscoverNotes(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        size = Math.min(size, 50);
        List<NoteVO> list = noteService.getDiscoverNotes(cursor, size, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", list);
        result.put("hasMore", list.size() == size);
        
        // 生成下一页游标（已展示的笔记ID列表）
        if (!list.isEmpty() && list.size() == size) {
            String nextCursor = list.stream()
                .map(note -> note.getId().toString())
                .collect(Collectors.joining(","));
            result.put("nextCursor", nextCursor);
        }
        
        return R.ok(result);
    }
    
    /**
     * 热门 - 热度排序笔记（游标分页）
     */
    @Operation(summary = "热门笔记")
    @GetMapping("/popular")
    public R<Map<String, Object>> getPopularNotes(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        // 刷新热门笔记热度（带全局限流）
        noteService.refreshHotScoreIfNeeded();
        
        List<NoteVO> list = noteService.getPopularNotes(cursor, size, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("data", list);
        result.put("hasMore", list.size() == size);
        
        if (!list.isEmpty() && list.size() == size) {
            NoteVO lastNote = list.get(list.size() - 1);
            if (lastNote.getCreatedAt() != null) {
                String nextCursor = lastNote.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() + "_" + lastNote.getId();
                result.put("nextCursor", nextCursor);
            }
        }
        
        return R.ok(result);
    }
    
    /**
     * 获取当前用户的笔记列表
     */
    @Operation(summary = "获取我的笔记")
    @GetMapping("/my")
    public R<List<NoteVO>> getMyNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        List<NoteVO> list = noteService.getMyNotes(userId, page, size, userId);
        return R.ok(list);
    }
    
    /**
     * 获取指定用户的笔记列表
     */
    @Operation(summary = "获取用户笔记")
    @GetMapping("/notes-by/{userId}")
    public R<List<NoteVO>> getUserNotes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        List<NoteVO> list = noteService.getMyNotes(userId, page, size, currentUserId);
        return R.ok(list);
    }
    
    /**
     * 获取当前用户的收藏列表
     */
    @Operation(summary = "获取我的收藏")
    @GetMapping("/favorites")
    public R<List<NoteVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        List<NoteVO> list = noteService.getMyFavorites(userId, page, size, userId);
        return R.ok(list);
    }
    
    /**
     * 获取当前用户的获赞数
     */
    @Operation(summary = "获取我的获赞数")
    @GetMapping("/my/likes-count")
    public R<Long> getMyLikesCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        long count = noteService.getMyLikesCount(userId);
        return R.ok(count);
    }
    
    /**
     * 获取指定用户的获赞数
     */
    @Operation(summary = "获取用户获赞数")
    @GetMapping("/user/{userId}/likes-count")
    public R<Long> getUserLikesCount(@PathVariable Long userId) {
        long count = noteService.getMyLikesCount(userId);
        return R.ok(count);
    }
    
    /**
     * 获取笔记详情
     */
    @Operation(summary = "获取笔记详情")
    @GetMapping("/{noteId}")
    public R<NoteVO> getNoteDetail(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        NoteVO note = noteService.getNoteDetail(noteId, userId);
        return R.ok(note);
    }
    
    /**
     * 删除笔记
     */
    @Operation(summary = "删除笔记")
    @DeleteMapping("/{noteId}")
    public R<?> deleteNote(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        boolean success = noteService.deleteNote(noteId, userId);
        return success ? R.ok("删除成功") : R.fail("删除失败");
    }
    
    /**
     * 点赞笔记 - 每用户每分钟最多5次
     */
    @Operation(summary = "点赞笔记")
    @RateLimit(
        key = "rate:like:user:{userId}:note:{noteId}",
        maxRequests = 5,
        windowSeconds = 60,
        message = "您的点赞操作频繁，请稍后重试"
    )
    @PostMapping("/{noteId}/like")
    public R<?> likeNote(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }

        try {
            java.util.Map<String, Object> result = noteService.likeNote(noteId, userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 取消点赞 - 每用户每分钟最多5次
     */
    @Operation(summary = "取消点赞")
    @RateLimit(
        key = "rate:unlike:user:{userId}:note:{noteId}",
        maxRequests = 5,
        windowSeconds = 60,
        message = "您的取消点赞操作频繁，请稍后重试"
    )
    @DeleteMapping("/{noteId}/like")
    public R<?> unlikeNote(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }

        try {
            java.util.Map<String, Object> result = noteService.unlikeNote(noteId, userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 收藏笔记 - 每用户每分钟最多3次
     */
    @Operation(summary = "收藏笔记")
    @RateLimit(
        key = "rate:favorite:user:{userId}:note:{noteId}",
        maxRequests = 3,
        windowSeconds = 60,
        message = "您的收藏操作频繁，请稍后重试"
    )
    @PostMapping("/{noteId}/favorite")
    public R<?> favoriteNote(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        try {
            java.util.Map<String, Object> result = noteService.favoriteNote(noteId, userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }

    /**
     * 取消收藏 - 每用户每分钟最多3次
     */
    @Operation(summary = "取消收藏")
    @RateLimit(
        key = "rate:unfavorite:user:{userId}:note:{noteId}",
        maxRequests = 3,
        windowSeconds = 60,
        message = "您的取消收藏操作频繁，请稍后重试"
    )
    @DeleteMapping("/{noteId}/favorite")
    public R<?> unfavoriteNote(
            @PathVariable Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        try {
            java.util.Map<String, Object> result = noteService.unfavoriteNote(noteId, userId);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail(400, e.getMessage());
        }
    }
    
    /**
     * 添加评论
     */
    @Operation(summary = "添加评论")
    @PostMapping("/comment")
    public R<NoteComment> addComment(
            @Valid @RequestBody CreateCommentRequest requestBody,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        Long noteId = requestBody.getNoteId();
        Long parentId = requestBody.getParentId();
        boolean isRoot = (parentId == null || parentId == 0);
        
        NoteComment comment = commentService.addComment(userId, requestBody);
        
        if (comment != null) {
            fullSortStrategy.addCommentToTree(noteId, comment, isRoot);
        }
        
        return R.ok("评论成功", comment);
    }
    
    /**
     * 获取评论列表（使用Redis缓存的树状结构）
     */
    @Operation(summary = "获取评论列表")
    @GetMapping("/{noteId}/comments")
    public R<CommentTreeResponse> getComments(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "hottest") String sort) {
        
        size = Math.min(size, 20);
        
        CommentTreeResponse response = fullSortStrategy.getCommentTree(noteId, sort, cursor, size);
        
        return R.ok(response);
    }

    /**
     * 获取AI回复建议
     * 用户发评论后调用，获取AI生成的回复建议
     */
    @Operation(summary = "获取AI回复建议")
    @GetMapping("/comment/{commentId}/ai-suggestions")
    public R<List<String>> getAiReplySuggestions(@PathVariable Long commentId) {
        // 获取评论信息
        NoteComment comment = commentService.getCommentById(commentId);
        if (comment == null) {
            return R.fail(404, "评论不存在");
        }

        // 获取笔记内容
        Long noteId = comment.getNoteId();
        Note note = noteService.getNoteById(noteId);
        if (note == null) {
            return R.fail(404, "笔记不存在");
        }

        // 生成AI建议
        List<String> suggestions = aiReplySuggestionService.getSuggestions(
                noteId, commentId, note.getTitle(), comment.getContent());

        return R.ok(suggestions);
    }

    /**
     * 删除评论
     * 支持删除自己发布的评论，或笔记发布者删除任意评论
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public R<?> deleteComment(
            @PathVariable Long commentId,
            @RequestParam(required = false) Long noteId,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return R.fail(401, "请先登录");
        }
        
        // 如果提供了noteId，检查是否是笔记发布者
        boolean isNoteOwner = false;
        if (noteId != null) {
            Note note = noteService.getNoteById(noteId);
            if (note != null && userId.equals(note.getUserId())) {
                isNoteOwner = true;
            }
        }
        
        boolean success = commentService.deleteComment(commentId, userId, isNoteOwner);
        
        if (success && noteId != null) {
            // 获取被删除评论的parentId，用于判断是否需要级联删除
            NoteComment deletedComment = commentSortService.getCommentById(commentId);
            Long parentId = deletedComment != null ? deletedComment.getParentId() : null;
            fullSortStrategy.removeCommentAndChildrenFromTree(noteId, commentId, parentId);
        }
        
        return success ? R.ok("删除成功") : R.fail("删除失败");
    }
    
    /**
     * 上传图片
     */
    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            return R.fail("文件大小不能超过5MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.fail("只能上传图片文件");
        }
        
        try {
            String imageUrl = ossService.uploadImage(file);
            return R.ok(imageUrl);
        } catch (Exception e) {
            return R.fail("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传视频
     */
    @Operation(summary = "上传视频")
    @PostMapping("/upload-video")
    public R<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        
        try {
            String videoUrl = ossService.uploadVideo(file);
            return R.ok(videoUrl);
        } catch (Exception e) {
            return R.fail("视频上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传视频（带自动封面提取）
     */
    @Operation(summary = "上传视频并自动提取封面")
    @PostMapping("/upload-video-with-cover")
    public R<Map<String, String>> uploadVideoWithCover(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }
        
        try {
            Map<String, String> result = ossService.uploadVideoWithCover(file);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("视频上传失败: " + e.getMessage());
        }
    }
}
