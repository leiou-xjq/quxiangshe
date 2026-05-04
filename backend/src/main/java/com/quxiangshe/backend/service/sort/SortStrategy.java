package com.quxiangshe.backend.service.sort;

import com.quxiangshe.backend.entity.CommentSortData;
import java.util.List;

public interface SortStrategy {
    List<CommentSortData> getRootComments(Long postId, String sort, String cursor, int size);
    List<CommentSortData> getAllComments(Long postId, String sort, String cursor, int size);
    List<CommentSortData> getChildComments(Long postId, Long rootId, String sort, String cursor, int size);
    void addComment(CommentSortData comment);
    void updateScore(Long commentId, double score);
    void removeComment(Long commentId);
    long getCommentCount(Long postId, Long rootId);
}