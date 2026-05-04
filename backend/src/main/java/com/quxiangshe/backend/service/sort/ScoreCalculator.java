package com.quxiangshe.backend.service.sort;

import com.quxiangshe.backend.entity.CommentSortData;

public class ScoreCalculator {
    private static final double LIKE_WEIGHT = 2.0;
    private static final double REPLY_WEIGHT = 3.0;
    private static final double TIME_DECAY = 0.1;
    
    public static double calculateHotScore(CommentSortData comment) {
        if (comment == null) return 0;
        double baseScore = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) * LIKE_WEIGHT + 
                          (comment.getReplyCount() != null ? comment.getReplyCount() : 0) * REPLY_WEIGHT;
        if (comment.getCreatedAt() != null) {
            long hoursSince = (System.currentTimeMillis() - comment.getCreatedAt()) / 3600000;
            baseScore -= hoursSince * TIME_DECAY;
        }
        return baseScore;
    }
    
    public static double calculateTimeScore(CommentSortData comment) {
        return comment != null && comment.getCreatedAt() != null ? comment.getCreatedAt() : 0;
    }
    
    public static double calculateTimeDescScore(CommentSortData comment) {
        return comment != null && comment.getCreatedAt() != null ? -comment.getCreatedAt() : 0;
    }
    
    public static boolean isHotComment(CommentSortData comment, int hotThreshold) {
        if (comment == null) return false;
        double score = (comment.getLikeCount() != null ? comment.getLikeCount() : 0) * LIKE_WEIGHT + 
                      (comment.getReplyCount() != null ? comment.getReplyCount() : 0) * REPLY_WEIGHT;
        return score >= hotThreshold;
    }
}