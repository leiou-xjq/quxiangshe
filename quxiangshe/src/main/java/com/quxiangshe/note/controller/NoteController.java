package com.quxiangshe.note.controller;

import com.quxiangshe.note.dto.NoteCreateRequestDTO;
import com.quxiangshe.note.dto.NoteResponseDTO;
import com.quxiangshe.note.service.NoteService;
import com.quxiangshe.common.dto.Response;
import com.quxiangshe.common.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping({"/notes", "/posts"})
    @RateLimit(keyPrefix = "note:create", limit = 10, windowMs = 60000)
    public Response<NoteResponseDTO> createNote(
            @Validated @RequestBody NoteCreateRequestDTO request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("创建笔记: userId={}", userId);
        return Response.success(noteService.createNote(userId, request));
    }

    @GetMapping({"/notes/{noteId}", "/posts/{noteId}"})
    public Response<NoteResponseDTO> getNoteDetail(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long currentUserId = null;
        if (authentication != null) {
            currentUserId = Long.parseLong(authentication.getName());
        }
        return Response.success(noteService.getNoteDetail(noteId, currentUserId));
    }

    @GetMapping({"/users/{userId}/notes", "/users/{userId}/posts"})
    public Response<NoteService.NoteListResponse> getUserNotes(
            @PathVariable Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") Integer size) {
        return Response.success(noteService.getUserNotes(userId, lastId, size));
    }

    @GetMapping({"/notes", "/posts"})
    public Response<NoteService.NoteListResponse> getHomeNotes(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") Integer size) {
        return Response.success(noteService.getHomeNotes(lastId, size));
    }

    @GetMapping({"/notes/search", "/posts/search"})
    public Response<NoteService.NoteListResponse> searchNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Response.success(noteService.searchNotes(keyword, category, page, size));
    }

    @PostMapping({"/notes/{noteId}/like", "/posts/{noteId}/like"})
    @RateLimit(keyPrefix = "note:like", limit = 20, windowMs = 60000)
    public Response<Void> likeNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        noteService.likeNote(userId, noteId);
        return Response.success();
    }

    @DeleteMapping({"/notes/{noteId}/like", "/posts/{noteId}/like"})
    public Response<Void> unlikeNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        noteService.unlikeNote(userId, noteId);
        return Response.success();
    }

    @PostMapping({"/notes/{noteId}/collect", "/posts/{noteId}/collect"})
    public Response<Void> collectNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        noteService.collectNote(userId, noteId);
        return Response.success();
    }

    @DeleteMapping({"/notes/{noteId}/collect", "/posts/{noteId}/collect"})
    public Response<Void> uncollectNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        noteService.uncollectNote(userId, noteId);
        return Response.success();
    }

    @DeleteMapping({"/notes/{noteId}", "/posts/{noteId}"})
    public Response<Void> deleteNote(
            @PathVariable Long noteId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        noteService.deleteNote(userId, noteId);
        return Response.success();
    }

    @PostMapping({"/admin/notes/{noteId}/review", "/admin/posts/{noteId}/review"})
    public Response<Void> reviewNote(
            @PathVariable Long noteId,
            @RequestParam boolean approved) {
        noteService.reviewNote(noteId, approved);
        return Response.success();
    }
}
