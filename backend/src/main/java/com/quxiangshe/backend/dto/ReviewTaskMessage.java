package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 审核任务消息
 * 通过MQ投递审核任务，保证可靠性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long noteId;

    private Long userId;

    private String title;

    private String content;

    private List<String> imageUrls;

    private Long submitTime;
}
