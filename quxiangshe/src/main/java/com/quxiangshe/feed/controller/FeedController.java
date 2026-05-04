package com.quxiangshe.feed.controller;

import com.quxiangshe.common.dto.Response;
import com.quxiangshe.feed.dto.FeedResponseDTO;
import com.quxiangshe.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Feed流控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 获取Feed流（游标分页）
     *
     * @param cursor      游标ID（上次返回的lastPostId）
     * @param postTime   游标时间（上次返回的lastPostTime）
     * @param size        每页数量
     * @return Feed流响应
     */
    @GetMapping
    public Response<FeedResponseDTO> getFeed(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long postTime,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        
        Long userId = Long.parseLong(authentication.getName());
        log.info("获取Feed流: userId={}, cursor={}, postTime={}, size={}", userId, cursor, postTime, size);
        
        return Response.success(feedService.getFeed(userId, cursor, postTime, size));
    }
}
