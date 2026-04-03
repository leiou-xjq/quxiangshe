package com.quxiangshe.note.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.comment.service.SensitiveWordService;
import com.quxiangshe.note.document.NoteDocument;
import com.quxiangshe.note.dto.NoteCreateRequestDTO;
import com.quxiangshe.note.dto.NoteResponseDTO;
import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.entity.NoteImageEntity;
import com.quxiangshe.note.entity.NoteLikeEntity;
import com.quxiangshe.note.entity.NoteCollectEntity;
import com.quxiangshe.note.entity.SensitiveCheckLogEntity;
import com.quxiangshe.note.mapper.NoteImageMapper;
import com.quxiangshe.note.mapper.NoteLikeMapper;
import com.quxiangshe.note.mapper.NoteCollectMapper;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.note.mapper.SensitiveCheckLogMapper;
import com.quxiangshe.note.queue.NoteQueueProducer;
import com.quxiangshe.note.service.NoteSearchService;
import com.quxiangshe.note.service.NoteService;
import com.quxiangshe.common.exception.BusinessException;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记服务实现类
 * 核心功能：
 * 1. 敏感词校验（DFA算法）
 * 2. 事务管理
 * 3. ES异步同步 + 重试机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final NoteImageMapper noteImageMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCollectMapper noteCollectMapper;
    private final SensitiveCheckLogMapper sensitiveCheckLogMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final NoteSearchService noteSearchService;
    private final NoteQueueProducer queueProducer;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final int SENSITIVE_WORD_REJECT_THRESHOLD = 3;
    private static final String LIKE_CACHE_KEY_PREFIX = "note:like:";
    private static final String COLLECT_CACHE_KEY_PREFIX = "note:collect:";
    private static final long CACHE_EXPIRE_SECONDS = 3600;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteResponseDTO createNote(Long userId, NoteCreateRequestDTO request) {
        long startTime = System.currentTimeMillis();
        log.info("开始创建笔记: userId={}, title={}", userId, request.getTitle());

        // ========== 第1步：敏感词校验 ==========
        SensitiveWordCheckResult checkResult = checkSensitiveWords(userId, request);

        // 根据校验结果决定审核状态
        int auditStatus;
        String rejectReason = null;

        if (checkResult.isRejected()) {
            auditStatus = NoteEntity.AUDIT_REJECTED;
            rejectReason = "敏感词数量超过阈值";
        } else if (checkResult.isReplaced()) {
            auditStatus = NoteEntity.AUDIT_PASSED;
        } else {
            auditStatus = NoteEntity.AUDIT_PASSED;
        }

        int status = (auditStatus == NoteEntity.AUDIT_PASSED) 
            ? NoteEntity.STATUS_NORMAL 
            : NoteEntity.STATUS_VIOLATION;

        // ========== 第2步：创建笔记实体 ==========
        String tagsJson = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                tagsJson = objectMapper.writeValueAsString(request.getTags());
            } catch (Exception e) {
                log.warn("序列化标签失败", e);
            }
        }

        NoteEntity note = NoteEntity.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .category(request.getCategory() != null ? request.getCategory() : "默认")
                .tags(tagsJson)
                .likeCount(0)
                .commentCount(0)
                .collectCount(0)
                .viewCount(0)
                .status(status)
                .auditStatus(auditStatus)
                .rejectReason(rejectReason)
                .deleted(NoteEntity.DELETED_NO)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // ========== 第3步：保存笔记 ==========
        noteMapper.insert(note);
        log.info("笔记已保存到数据库: noteId={}, userId={}, auditStatus={}", 
                note.getId(), userId, auditStatus);

        // ========== 第4步：保存图片 ==========
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (int i = 0; i < request.getImages().size(); i++) {
                NoteImageEntity image = NoteImageEntity.builder()
                        .noteId(note.getId())
                        .imageUrl(request.getImages().get(i))
                        .imageOrder(i)
                        .createTime(LocalDateTime.now())
                        .build();
                noteImageMapper.insert(image);
            }
        }

        // ========== 第5步：保存敏感词校验日志 ==========
        saveSensitiveCheckLog(userId, note.getId(), checkResult);

        // ========== 第6步：异步同步到ES（仅审核通过） ==========
        if (auditStatus == NoteEntity.AUDIT_PASSED) {
            syncToElasticsearch(note);
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("创建笔记完成: noteId={}, userId={}, status={}, costTime={}ms", 
                note.getId(), userId, status, costTime);

        // ========== 第7：构建返回结果 ==========
        NoteResponseDTO result = NoteResponseDTO.builder()
                .noteId(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .auditStatus(auditStatus)
                .rejectReason(rejectReason)
                .createTime(note.getCreateTime().toString())
                .build();

        return result;
    }

    @Override
    public NoteResponseDTO getNoteDetail(Long noteId, Long currentUserId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        // 审核通过且状态正常才展示
        if (note.getAuditStatus() != NoteEntity.AUDIT_PASSED || note.getStatus() != NoteEntity.STATUS_NORMAL) {
            throw new BusinessException("笔记不存在");
        }

        noteMapper.incrementViewCount(noteId);

        List<NoteImageEntity> images = noteImageMapper.selectByNoteId(noteId);
        UserEntity user = userMapper.selectById(note.getUserId());

        return buildNoteDTO(note, images, user, currentUserId);
    }

    @Override
    public NoteListResponse getUserNotes(Long userId, Long lastId, Integer size) {
        if (size == null || size <= 0) {
            size = 20;
        }

        Long cursor = (lastId != null && lastId > 0) ? lastId : null;
        List<NoteEntity> notes = noteMapper.selectByUserId(userId, cursor, size + 1);

        boolean hasMore = notes.size() > size;
        if (hasMore) {
            notes = notes.subList(0, size);
        }

        if (notes.isEmpty()) {
            return new NoteListResponse(Collections.emptyList(), null, false);
        }

        Set<Long> userIds = notes.stream().map(NoteEntity::getUserId).collect(Collectors.toSet());
        Map<Long, UserEntity> userMap = getUserMap(userIds);

        List<NoteResponseDTO> items = notes.stream()
                .map(n -> buildNoteDTO(n, null, userMap.get(n.getUserId()), null))
                .collect(Collectors.toList());

        Long newLastNoteId = null;
        if (!notes.isEmpty()) {
            newLastNoteId = notes.get(notes.size() - 1).getId();
        }

        return new NoteListResponse(items, newLastNoteId, hasMore);
    }

    @Override
    public NoteListResponse getHomeNotes(Long lastId, Integer size) {
        if (size == null || size <= 0) {
            size = 20;
        }

        Long cursor = (lastId != null && lastId > 0) ? lastId : null;
        List<NoteEntity> notes = noteMapper.selectForHome(cursor, size + 1);

        boolean hasMore = notes.size() > size;
        if (hasMore) {
            notes = notes.subList(0, size);
        }

        if (notes.isEmpty()) {
            return new NoteListResponse(Collections.emptyList(), null, false);
        }

        Set<Long> userIds = notes.stream().map(NoteEntity::getUserId).collect(Collectors.toSet());
        Map<Long, UserEntity> userMap = getUserMap(userIds);

        List<NoteResponseDTO> items = notes.stream()
                .map(n -> buildNoteDTO(n, null, userMap.get(n.getUserId()), null))
                .collect(Collectors.toList());

        Long newLastNoteId = null;
        if (!notes.isEmpty()) {
            newLastNoteId = notes.get(notes.size() - 1).getId();
        }

        return new NoteListResponse(items, newLastNoteId, hasMore);
    }

    @Override
    public NoteListResponse searchNotes(String keyword, String category, Integer page, Integer size) {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }

        List<NoteResponseDTO> items = noteSearchService.searchNotes(keyword, category, null, page, size);

        boolean hasMore = items.size() > size;
        if (hasMore) {
            items = items.subList(0, size);
        }

        Long lastNoteId = null;
        if (!items.isEmpty()) {
            lastNoteId = items.get(items.size() - 1).getNoteId();
        }

        return new NoteListResponse(items, lastNoteId, hasMore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        if (checkUserLikedFromCache(noteId, userId) || noteLikeMapper.checkUserLiked(noteId, userId)) {
            throw new BusinessException("已点赞");
        }

        NoteLikeEntity like = NoteLikeEntity.builder()
                .noteId(noteId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build();
        noteLikeMapper.insert(like);
        noteMapper.incrementLikeCount(noteId);

        setUserLikedCache(noteId, userId, true);
        log.info("用户 {} 点赞笔记 {}", userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        if (!checkUserLikedFromCache(noteId, userId) && !noteLikeMapper.checkUserLiked(noteId, userId)) {
            throw new BusinessException("未点赞");
        }

        noteLikeMapper.deleteByUserAndNote(noteId, userId);
        noteMapper.decrementLikeCount(noteId);

        setUserLikedCache(noteId, userId, false);
        log.info("用户 {} 取消点赞笔记 {}", userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        if (checkUserCollectedFromCache(noteId, userId) || noteCollectMapper.checkUserCollected(noteId, userId)) {
            throw new BusinessException("已收藏");
        }

        NoteCollectEntity collect = NoteCollectEntity.builder()
                .noteId(noteId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build();
        noteCollectMapper.insert(collect);
        noteMapper.incrementCollectCount(noteId);

        setUserCollectedCache(noteId, userId, true);
        log.info("用户 {} 收藏笔记 {}", userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || note.getDeleted() == 1) {
            throw new BusinessException("笔记不存在");
        }

        if (!checkUserCollectedFromCache(noteId, userId) && !noteCollectMapper.checkUserCollected(noteId, userId)) {
            throw new BusinessException("未收藏");
        }

        noteCollectMapper.deleteByUserAndNote(noteId, userId);
        noteMapper.decrementCollectCount(noteId);

        setUserCollectedCache(noteId, userId, false);
        log.info("用户 {} 取消收藏笔记 {}", userId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        if (!note.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        note.setDeleted(NoteEntity.DELETED_YES);
        note.setStatus(NoteEntity.STATUS_USER_DELETED);
        noteMapper.updateById(note);

        try {
            queueProducer.sendNoteMessage(note);
        } catch (Exception e) {
            log.error("发送笔记删除消息失败: noteId={}", noteId, e);
        }

        log.info("笔记删除成功: noteId={}, userId={}", noteId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewNote(Long noteId, boolean approved) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }

        if (approved) {
            note.setAuditStatus(NoteEntity.AUDIT_PASSED);
            note.setStatus(NoteEntity.STATUS_NORMAL);
            noteMapper.updateById(note);

            syncToElasticsearch(note);
        } else {
            note.setAuditStatus(NoteEntity.AUDIT_REJECTED);
            note.setStatus(NoteEntity.STATUS_VIOLATION);
            note.setDeleted(NoteEntity.DELETED_YES);
            note.setRejectReason("管理员审核拒绝");
            noteMapper.updateById(note);

            try {
                queueProducer.sendNoteMessage(note);
            } catch (Exception e) {
                log.error("审核拒绝后发送ES删除消息失败: noteId={}", noteId, e);
            }
        }

        log.info("笔记审核完成: noteId={}, approved={}", noteId, approved);
    }

    // ========== 私有方法 ==========

    private SensitiveWordCheckResult checkSensitiveWords(Long userId, NoteCreateRequestDTO request) {
        SensitiveWordCheckResult result = new SensitiveWordCheckResult();

        String originalTitle = request.getTitle();
        String originalContent = request.getContent();

        Set<String> titleWords = sensitiveWordService.findSensitiveWords(originalTitle);
        Set<String> contentWords = sensitiveWordService.findSensitiveWords(originalContent);

        Set<String> allWords = new HashSet<>(titleWords);
        allWords.addAll(contentWords);

        result.setTitleSensitiveWords(titleWords);
        result.setContentSensitiveWords(contentWords);
        result.setAllSensitiveWords(allWords);

        if (!allWords.isEmpty()) {
            if (titleWords.size() > 0) {
                request.setTitle(sensitiveWordService.replaceSensitiveWord(originalTitle));
                result.setReplaced(true);
            }
            if (contentWords.size() > 0) {
                request.setContent(sensitiveWordService.replaceSensitiveWord(originalContent));
                result.setReplaced(true);
            }
        }

        if (allWords.size() >= SENSITIVE_WORD_REJECT_THRESHOLD) {
            result.setRejected(true);
        }

        log.info("敏感词校验结果: userId={}, titleWords={}, contentWords={}, total={}, rejected={}",
                userId, titleWords.size(), contentWords.size(), allWords.size(), result.isRejected());

        return result;
    }

    private void saveSensitiveCheckLog(Long userId, Long noteId, SensitiveWordCheckResult checkResult) {
        try {
            for (String word : checkResult.getTitleSensitiveWords()) {
                SensitiveCheckLogEntity logEntity = SensitiveCheckLogEntity.builder()
                        .noteId(noteId)
                        .userId(userId)
                        .contentType(SensitiveCheckLogEntity.CONTENT_TYPE_TITLE)
                        .originalContent("")
                        .foundWords(word)
                        .checkResult(checkResult.isRejected() 
                            ? SensitiveCheckLogEntity.CHECK_RESULT_REJECTED 
                            : SensitiveCheckLogEntity.CHECK_RESULT_REPLACED)
                        .createTime(LocalDateTime.now())
                        .build();
                sensitiveCheckLogMapper.insert(logEntity);
            }

            for (String word : checkResult.getContentSensitiveWords()) {
                SensitiveCheckLogEntity logEntity = SensitiveCheckLogEntity.builder()
                        .noteId(noteId)
                        .userId(userId)
                        .contentType(SensitiveCheckLogEntity.CONTENT_TYPE_CONTENT)
                        .originalContent("")
                        .foundWords(word)
                        .checkResult(checkResult.isRejected() 
                            ? SensitiveCheckLogEntity.CHECK_RESULT_REJECTED 
                            : SensitiveCheckLogEntity.CHECK_RESULT_REPLACED)
                        .createTime(LocalDateTime.now())
                        .build();
                sensitiveCheckLogMapper.insert(logEntity);
            }
        } catch (Exception e) {
            log.warn("保存敏感词校验日志失败: noteId={}", noteId, e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void syncToElasticsearch(NoteEntity note) {
        try {
            queueProducer.sendNoteMessage(note);
            log.info("ES同步消息已发送: noteId={}", note.getId());
        } catch (Exception e) {
            log.error("ES同步失败，准备重试: noteId={}", note.getId(), e);
            throw e;
        }
    }

    private Map<Long, UserEntity> getUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, u -> u));
    }

    private NoteResponseDTO buildNoteDTO(NoteEntity note, List<NoteImageEntity> images,
                                          UserEntity user, Long currentUserId) {
        List<String> tagsList = null;
        if (note.getTags() != null) {
            try {
                tagsList = objectMapper.readValue(note.getTags(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析标签失败: {}", note.getTags());
            }
        }

        List<String> imageUrls = null;
        if (images != null && !images.isEmpty()) {
            imageUrls = images.stream()
                    .map(NoteImageEntity::getImageUrl)
                    .collect(Collectors.toList());
        }

        return NoteResponseDTO.builder()
                .noteId(note.getId())
                .userId(note.getUserId())
                .username(user != null ? user.getUsername() : "")
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : "")
                .title(note.getTitle())
                .content(note.getContent())
                .coverImage(note.getCoverImage())
                .category(note.getCategory())
                .tags(tagsList)
                .images(imageUrls)
                .likeCount(note.getLikeCount())
                .commentCount(note.getCommentCount())
                .collectCount(note.getCollectCount())
                .viewCount(note.getViewCount())
                .auditStatus(note.getAuditStatus())
                .rejectReason(note.getRejectReason())
                .isLiked(false)
                .isCollected(false)
                .createTime(note.getCreateTime() != null ? note.getCreateTime().toString() : "")
                .build();
    }

    private boolean checkUserLikedFromCache(Long noteId, Long userId) {
        try {
            String key = LIKE_CACHE_KEY_PREFIX + noteId + ":" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("读取点赞缓存失败: noteId={}, userId={}", noteId, userId);
            return false;
        }
    }

    private void setUserLikedCache(Long noteId, Long userId, boolean liked) {
        try {
            String key = LIKE_CACHE_KEY_PREFIX + noteId + ":" + userId;
            if (liked) {
                redisTemplate.opsForValue().set(key, "1", CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            } else {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("设置点赞缓存失败: noteId={}, userId={}", noteId, userId);
        }
    }

    private boolean checkUserCollectedFromCache(Long noteId, Long userId) {
        try {
            String key = COLLECT_CACHE_KEY_PREFIX + noteId + ":" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("读取收藏缓存失败: noteId={}, userId={}", noteId, userId);
            return false;
        }
    }

    private void setUserCollectedCache(Long noteId, Long userId, boolean collected) {
        try {
            String key = COLLECT_CACHE_KEY_PREFIX + noteId + ":" + userId;
            if (collected) {
                redisTemplate.opsForValue().set(key, "1", CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            } else {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("设置收藏缓存失败: noteId={}, userId={}", noteId, userId);
        }
    }

    // ========== 内部类 ==========

    private static class SensitiveWordCheckResult {
        private Set<String> titleSensitiveWords = new HashSet<>();
        private Set<String> contentSensitiveWords = new HashSet<>();
        private Set<String> allSensitiveWords = new HashSet<>();
        private boolean replaced = false;
        private boolean rejected = false;

        public Set<String> getTitleSensitiveWords() { return titleSensitiveWords; }
        public void setTitleSensitiveWords(Set<String> titleSensitiveWords) { this.titleSensitiveWords = titleSensitiveWords; }
        public Set<String> getContentSensitiveWords() { return contentSensitiveWords; }
        public void setContentSensitiveWords(Set<String> contentSensitiveWords) { this.contentSensitiveWords = contentSensitiveWords; }
        public Set<String> getAllSensitiveWords() { return allSensitiveWords; }
        public void setAllSensitiveWords(Set<String> allSensitiveWords) { this.allSensitiveWords = allSensitiveWords; }
        public boolean isReplaced() { return replaced; }
        public void setReplaced(boolean replaced) { this.replaced = replaced; }
        public boolean isRejected() { return rejected; }
        public void setRejected(boolean rejected) { this.rejected = rejected; }
    }
}
