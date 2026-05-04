package com.quxiangshe.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 评论排序配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "comment.sort")
public class CommentSortConfig {
    
    /** 切换分桶策略的数量阈值（设为最大值，不使用分桶策略） */
    private int threshold = Integer.MAX_VALUE;
    
    /** 每个桶最多取出的数据条数 */
    private int bucketSize = 100;
    
    /** 热点判定分数阈值 */
    private int hotThreshold = 50;
    
    /** 热点桶最少评论数 */
    private int hotMinComments = 10;
}