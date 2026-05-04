package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.entity.NoteComment;
import java.util.List;

public interface ICommentSortService {
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    
    List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size);
    
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    
    long getCommentCount(Long postId, Long rootId);
    
    CommentSortData addComment(Long userId, Long postId, Long parentId, String content);
    
    void likeComment(Long commentId, Long userId);
    
    void unlikeComment(Long commentId, Long userId);
    
    void deleteComment(Long commentId, Long userId);
    
    NoteComment getCommentById(Long commentId);
    
    void initCommentSort(Long postId);
    
    void verifyAllCommentSort();
}