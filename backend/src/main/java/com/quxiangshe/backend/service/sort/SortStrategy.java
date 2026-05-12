package com.quxiangshe.backend.service.sort;

import com.quxiangshe.backend.entity.CommentSortData;
import com.quxiangshe.backend.entity.CommentTreeResponse;
import com.quxiangshe.backend.entity.NoteComment;

import java.util.List;

public interface SortStrategy {
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size);
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    void addComment(CommentSortData comment);
    void updateScore(Long commentId, double score);
    void removeComment(Long commentId);
    long getCommentCount(Long postId, Long rootId);
    
    boolean addCommentToTree(Long postId, NoteComment comment, boolean isRoot);
    
    CommentTreeResponse getCommentTree(Long postId, String sort, String cursor, int size);
    
    boolean removeCommentAndChildrenFromTree(Long postId, Long commentId, Long parentId);
}