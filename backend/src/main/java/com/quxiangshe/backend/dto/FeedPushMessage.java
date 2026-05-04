package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPushMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long noteId;
    
    private Long authorId;
    
    private List<Long> targetUserIds;
    
    private int batchNum;
    
    private int totalBatches;
    
    private LocalDateTime pushTime;
}