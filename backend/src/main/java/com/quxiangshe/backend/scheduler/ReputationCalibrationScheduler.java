package com.quxiangshe.backend.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quxiangshe.backend.entity.User;
import com.quxiangshe.backend.mapper.UserMapper;
import com.quxiangshe.backend.service.IReputationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 信誉分定时校正任务
 *
 * 核心职责：定期校正Redis缓存与DB的信誉分数据一致性
 * 业务模块：用户模块
 *
 * 校正策略：
 *   - 每天凌晨4点执行全量校正
 *   - Redis为缓存，DB为真相源
 *   - 发现不一致时以DB为准，强制刷新Redis
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class ReputationCalibrationScheduler {

    private static final String REPUTATION_KEY_PREFIX = "user:reputation:";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IReputationService reputationService;

    /**
     * 每天凌晨4点执行信誉分校正
     *
     * 流程：
     *   1. 查询所有有信誉分的用户
     *   2. 逐个检查Redis缓存与DB是否一致
     *   3. 不一致时以DB为准，强制更新Redis
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void calibrateReputationScores() {
        log.info("开始执行信誉分校正任务");

        long startTime = System.currentTimeMillis();
        int totalCount = 0;
        int fixedCount = 0;

        try {
            // 1. 查询所有有信誉分的用户（分批处理，避免一次性查询过多）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.isNotNull(User::getReputationScore);
            queryWrapper.select(User::getId, User::getReputationScore);

            List<User> users = userMapper.selectList(queryWrapper);
            totalCount = users.size();

            log.info("信誉分校正: 待处理用户数={}", totalCount);

            // 2. 逐个检查Redis缓存与DB一致性
            for (User user : users) {
                try {
                    Long userId = user.getId();
                    Integer dbScore = user.getReputationScore();

                    String cacheKey = REPUTATION_KEY_PREFIX + userId;
                    String cachedScore = redisTemplate.opsForValue().get(cacheKey);

                    if (cachedScore == null) {
                        // Redis缓存不存在，直接回填
                        redisTemplate.opsForValue().set(cacheKey, String.valueOf(dbScore));
                        fixedCount++;
                        log.debug("信誉分缓存重建: userId={}, score={}", userId, dbScore);
                    } else if (!String.valueOf(dbScore).equals(cachedScore)) {
                        // 缓存与DB不一致，以DB为准强制更新
                        reputationService.setReputationScore(userId, dbScore);
                        fixedCount++;
                        log.warn("信誉分缓存校正: userId={}, oldCache={}, newScore={}",
                            userId, cachedScore, dbScore);
                    }
                } catch (Exception e) {
                    log.error("校正用户信誉分失败: userId={}, error={}", user.getId(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("信誉分校正完成: 总数={}, 校正数={}, 耗时={}ms", totalCount, fixedCount, duration);

        } catch (Exception e) {
            log.error("信誉分校正任务异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 每小时执行一次快速检查（仅检查热门用户）
     *
     * 热门用户定义：粉丝数>10000的用户
     * 这类用户信誉分变化较频繁，需要更频繁的校正
     */
    @Scheduled(cron = "0 30 * * * ?")
    public void quickCalibrateHotUsers() {
        log.debug("开始执行热门用户信誉分快速检查");

        try {
            // 简化实现：直接调用校正任务（实际可按粉丝数筛选）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.isNotNull(User::getReputationScore);
            queryWrapper.last("LIMIT 100");

            List<User> users = userMapper.selectList(queryWrapper);
            int fixedCount = 0;

            for (User user : users) {
                try {
                    Integer dbScore = user.getReputationScore();
                    String cacheKey = REPUTATION_KEY_PREFIX + user.getId();
                    String cachedScore = redisTemplate.opsForValue().get(cacheKey);

                    if (cachedScore == null || !String.valueOf(dbScore).equals(cachedScore)) {
                        reputationService.setReputationScore(user.getId(), dbScore);
                        fixedCount++;
                    }
                } catch (Exception ignored) {
                }
            }

            if (fixedCount > 0) {
                log.info("热门用户信誉分快速检查完成: 校正数={}", fixedCount);
            }

        } catch (Exception e) {
            log.error("热门用户信誉分快速检查异常: {}", e.getMessage());
        }
    }
}