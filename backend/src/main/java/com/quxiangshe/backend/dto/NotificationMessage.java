package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_REVIEW_PASSED = "REVIEW_PASSED";
    public static final String TYPE_REVIEW_REJECTED = "REVIEW_REJECTED";

    private String type;

    private Long userId;

    private Long fromUserId;

    private Long noteId;

    private Long commentId;

    private String extra;

    private LocalDateTime timestamp;
}