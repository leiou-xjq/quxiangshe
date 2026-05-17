package com.quxiangshe.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed推送消息DTO，用于通过消息队列（MQ）异步投递Feed推送任务。
 * <p>
 * 当博主发布新笔记时，系统将推送任务拆分为多个批次投递到MQ，
 * 每个批次包含一批目标用户ID。消费者根据推送模式（推/拉/推拉结合）
 * 将笔记投递到对应粉丝的收件箱或发件箱。
 * </p>
 *
 * @author 趣享社技术团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPushMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 推送的笔记ID */
    private Long noteId;
    
    /** 笔记作者ID */
    private Long authorId;
    
    /** 本批次的目标用户ID列表 */
    private List<Long> targetUserIds;
    
    /** 当前批次号（从1开始） */
    private int batchNum;
    
    /** 总批次数 */
    private int totalBatches;
    
    /** 推送发起时间 */
    private LocalDateTime pushTime;
}