package com.quxiangshe.feed.service.impl;

import com.quxiangshe.common.util.RedisUtil;
import com.quxiangshe.feed.dto.FeedItemDTO;
import com.quxiangshe.feed.dto.FeedResponseDTO;
import com.quxiangshe.feed.entity.FeedEntity;
import com.quxiangshe.feed.mapper.FeedMapper;
import com.quxiangshe.feed.service.FeedService;
import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.user.mapper.UserFollowMapper;
import com.quxiangshe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final RedisUtil redisUtil;
    private final FeedMapper feedMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;

    private static final String INBOX_KEY_PREFIX = "inbox:";

    @Value("${feed.inbox-size:200}")
    private int inboxSize;

    @Value("${feed.inbox-ttl:7}")
    private int inboxTtl;

    @Value("${feed.push-threshold:1000}")
    private int pushThreshold;

    @Override
    public FeedResponseDTO getFeed(Long userId, Long lastPostId, Long lastPostTime, Integer size) {
        if (size == null || size <= 0) {
            size = 20;
        }
        if (size > 50) {
            size = 50;
        }

        List<Long> noteIds = pullFromInbox(userId, lastPostId, size);
        
        if (!noteIds.isEmpty()) {
            return buildFeedResponse(noteIds, userId, size);
        }

        log.info("Redis收件箱为空，切换到MySQL拉取模式: userId={}", userId);
        return pullFromDB(userId, lastPostId, lastPostTime, size);
    }

    private List<Long> pullFromInbox(Long userId, Long lastPostId, int size) {
        String inboxKey = INBOX_KEY_PREFIX + userId;
        
        Long inboxSizeVal = redisUtil.lSize(inboxKey);
        if (inboxSizeVal == null || inboxSizeVal == 0) {
            return Collections.emptyList();
        }

        if (lastPostId != null) {
            List<Object> allNotes = redisUtil.lRange(inboxKey, 0, -1);
            int startIndex = -1;
            for (int i = 0; i < allNotes.size(); i++) {
                if (lastPostId.toString().equals(allNotes.get(i).toString())) {
                    startIndex = i + 1;
                    break;
                }
            }
            if (startIndex > 0 && startIndex < allNotes.size()) {
                Collections.reverse((ArrayList<?>) allNotes);
                allNotes = redisUtil.lRange(inboxKey, startIndex, startIndex + size - 1);
            }
        }

        List<Object> noteIdObjs = redisUtil.lRange(inboxKey, 0, size - 1);
        
        return noteIdObjs.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    private FeedResponseDTO buildFeedResponse(List<Long> noteIds, Long userId, int size) {
        if (noteIds == null || noteIds.isEmpty()) {
            return FeedResponseDTO.builder()
                    .items(Collections.emptyList())
                    .hasMore(false)
                    .build();
        }

        List<NoteEntity> notes = noteMapper.selectBatchIds(noteIds);
        if (notes == null || notes.isEmpty()) {
            return FeedResponseDTO.builder()
                    .items(Collections.emptyList())
                    .hasMore(false)
                    .build();
        }

        Map<Long, NoteEntity> noteMap = notes.stream()
                .collect(Collectors.toMap(NoteEntity::getId, p -> p));
        notes.sort(Comparator.comparingInt(p -> noteIds.indexOf(p.getId())));

        notes = notes.stream()
                .filter(p -> p.getDeleted() == 0 && p.getStatus() == NoteEntity.STATUS_NORMAL)
                .collect(Collectors.toList());

        Set<Long> userIds = notes.stream()
                .map(NoteEntity::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserEntity> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<FeedItemDTO> items = notes.stream()
                .map(note -> buildFeedItem(note, userMap.get(note.getUserId())))
                .collect(Collectors.toList());

        Long lastNoteId = notes.isEmpty() ? null : notes.get(notes.size() - 1).getId();
        Long lastNoteTime = notes.isEmpty() ? null : notes.get(notes.size() - 1).getCreateTime()
                .atZone(ZoneId.systemDefault()).toEpochSecond();

        return FeedResponseDTO.builder()
                .items(items)
                .lastPostId(lastNoteId)
                .lastPostTime(lastNoteTime)
                .hasMore(items.size() >= size)
                .build();
    }

    private FeedItemDTO buildFeedItem(NoteEntity note, UserEntity user) {
        return FeedItemDTO.builder()
                .postId(note.getId())
                .userId(note.getUserId())
                .username(user != null ? user.getUsername() : "")
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : "")
                .content(note.getContent())
                .mediaUrls(Collections.singletonList(note.getCoverImage()))
                .likeCount(note.getLikeCount())
                .commentCount(note.getCommentCount())
                .shareCount(note.getCollectCount())
                .isLiked(false)
                .createdAt(note.getCreateTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .createdTime(note.getCreateTime().toString())
                .build();
    }

    @Override
    public void pushToInbox(Long postId, Long creatorId) {
        List<Long> followers = userFollowMapper.selectFollowerIds(creatorId);
        if (followers == null || followers.isEmpty()) {
            log.debug("发布者无粉丝，无需推送: creatorId={}", creatorId);
            return;
        }

        if (followers.size() > pushThreshold) {
            log.info("粉丝数过多，不使用推模式: creatorId={}, followers={}", creatorId, followers.size());
            return;
        }

        String inboxKey = INBOX_KEY_PREFIX;
        for (Long followerId : followers) {
            try {
                String key = inboxKey + followerId;
                redisUtil.lPush(key, postId.toString());
                redisUtil.expire(key, inboxTtl * 24L * 3600L, java.util.concurrent.TimeUnit.SECONDS);
                trimInbox(key);
            } catch (Exception e) {
                log.error("写入收件箱失败: followerId={}, postId={}", followerId, postId, e);
            }
        }

        log.info("Feed推模式完成: postId={}, followers={}", postId, followers.size());
    }

    private void trimInbox(String inboxKey) {
        Long size = redisUtil.lSize(inboxKey);
        if (size != null && size > inboxSize) {
            redisTemplate.opsForList().trim(inboxKey, 0, inboxSize - 1);
        }
    }

    @jakarta.annotation.Resource
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Override
    public FeedResponseDTO pullFromDB(Long userId, Long lastPostId, Long lastPostTime, Integer size) {
        if (size == null || size <= 0) {
            size = 20;
        }

        LocalDateTime cursorTime = null;
        if (lastPostTime != null) {
            cursorTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(lastPostTime), ZoneId.systemDefault());
        }

        List<FeedEntity> feeds = feedMapper.selectUserFeed(userId, cursorTime, lastPostId, size + 1);
        
        if (feeds == null || feeds.isEmpty()) {
            return FeedResponseDTO.builder()
                    .items(Collections.emptyList())
                    .hasMore(false)
                    .build();
        }

        boolean hasMore = feeds.size() > size;
        if (hasMore) {
            feeds = feeds.subList(0, size);
        }

        List<Long> noteIds = feeds.stream()
                .map(FeedEntity::getPostId)
                .collect(Collectors.toList());

        return buildFeedResponse(noteIds, userId, size);
    }
}
