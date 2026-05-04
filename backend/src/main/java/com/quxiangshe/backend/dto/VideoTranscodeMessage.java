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
public class VideoTranscodeMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long noteId;

    private String originalUrl;

    private String targetFormat;

    private Integer targetWidth;

    private Integer targetHeight;

    private LocalDateTime timestamp;
}