package com.quxiangshe.user.controller;

import com.quxiangshe.common.dto.Response;
import com.quxiangshe.common.util.JwtUtil;
import com.quxiangshe.user.service.UserService;
import com.quxiangshe.user.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Response<UserProfileVO> getCurrentUser(HttpServletRequest request) {
        Long userId = JwtUtil.getUserIdFromRequest(request);
        return Response.success(userService.getCurrentUser(userId));
    }

    @GetMapping("/{userId}/profile")
    public Response<UserProfileVO> getUserProfile(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = null;
        try {
            currentUserId = JwtUtil.getUserIdFromRequest(request);
        } catch (Exception ignored) {
        }
        return Response.success(userService.getUserProfile(userId, currentUserId));
    }

    @PostMapping("/follow/{userId}")
    public Response<Void> follow(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = JwtUtil.getUserIdFromRequest(request);
        userService.follow(currentUserId, userId);
        return Response.success(null);
    }

    @DeleteMapping("/follow/{userId}")
    public Response<Void> unfollow(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = JwtUtil.getUserIdFromRequest(request);
        userService.unfollow(currentUserId, userId);
        return Response.success(null);
    }
}