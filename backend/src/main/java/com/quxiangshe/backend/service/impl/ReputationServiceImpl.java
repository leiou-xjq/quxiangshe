package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IReputationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户信誉分服务实现
 *
 * 核心设计：
 *   - Redis缓存：加速信誉分查询，TTL=1小时
 *   - DB真相源：User表reputation_score字段
 *   - 一致性策略：读优先Redis，更新先DB后异步刷Redis
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class ReputationServiceImpl implements IReputationService {

    private static final String REPUTATION_KEY_PREFIX = "user:reputation:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Value("${review.sync-review-threshold:80}")
    private int syncReviewThreshold;

    @Value("${reputation.max-score:100}")
    private int maxScore;

    @Value("${reputation.min-score:0}")
    private int minScore;

    @Value("${reputation.initial-score:50}")
    private int initialScore;

    @Value("${reputation.increase.review-passed:2}")
    private int reviewPassedBonus;

    @Value("${reputation.decrease.review-rejected:5}")
    private int reviewRejectedPenalty;

    @Override
    public int getReputationScore(Long userId) {
        String cacheKey = REPUTATION_KEY_PREFIX + userId;

        String cachedScore = redisTemplate.opsForValue().get(cacheKey);
        if (cachedScore != null) {
            log.debug("信誉分缓存命中: userId={}, score={}", userId, cachedScore);
            return Integer.parseInt(cachedScore);
        }

        com.quxiangshe.backend.entity.User user = userMapper.selectById(userId);
        int score = (user != null && user.getReputationScore() != null)
            ? user.getReputationScore()
            : initialScore;

        redisTemplate.opsForValue().set(cacheKey, String.valueOf(score), CACHE_TTL);
        log.debug("信誉分缓存未命中，从DB加载: userId={}, score={}", userId, score);

        return score;
    }

    @Override
    public void increaseReputation(Long userId, int delta) {
        if (delta <= 0) return;

        int currentScore = getReputationScore(userId);
        int newScore = Math.min(currentScore + delta, maxScore);

        updateDbAndCache(userId, newScore);
        log.info("信誉分增加: userId={}, {} -> {}, delta={}", userId, currentScore, newScore, delta);
    }

    @Override
    public void decreaseReputation(Long userId, int delta, String reason) {
        if (delta <= 0) return;

        int currentScore = getReputationScore(userId);
        int newScore = Math.max(currentScore - delta, minScore);

        updateDbAndCache(userId, newScore);
        log.info("信誉分减少: userId={}, {} -> {}, delta={}, reason={}",
            userId, currentScore, newScore, delta, reason);

        if (newScore < minScore) {
            log.warn("用户信誉分达到下限: userId={}, score={}", userId, newScore);
        }
    }

    @Override
    public void setReputationScore(Long userId, int score) {
        score = Math.min(Math.max(score, minScore), maxScore);
        updateDbAndCache(userId, score);
        log.info("信誉分重置: userId={}, score={}", userId, score);
    }

    @Override
    public boolean canSyncReview(Long userId) {
        return getReputationScore(userId) >= syncReviewThreshold;
    }

    private void updateDbAndCache(Long userId, int score) {
        com.quxiangshe.backend.entity.User user = new com.quxiangshe.backend.entity.User();
        user.setId(userId);
        user.setReputationScore(score);
        userMapper.updateById(user);

        String cacheKey = REPUTATION_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(score), CACHE_TTL);
    }
}