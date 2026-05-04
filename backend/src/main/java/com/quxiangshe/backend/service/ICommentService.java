package com.quxiangshe.backend.service;

import com.quxiangshe.backend.dto.CreateCommentRequest;
import com.quxiangshe.backend.entity.NoteComment;

import java.util.List;

public interface ICommentService {
    
    List<NoteComment> getComments(Long noteId, Long rootId);
    
    NoteComment addComment(Long userId, CreateCommentRequest requestBody);
    
    List<NoteComment> getCommentList(Long noteId, int page, int size);
    
    /**
     * @param commentId 评论ID
     * @param userId 用户ID（用于验证是否是评论发布者）
     * @param isNoteOwner 是否是笔记发布者（可以删除任意评论）
     * @return 是否成功
     */
    boolean deleteComment(Long commentId, Long userId, boolean isNoteOwner);
}