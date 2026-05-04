package com.quxiangshe.backend.service;

import com.quxiangshe.backend.entity.Report;

/**
 * 举报服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IReportService {
    
    /**
     * 提交举报
     * @param reporterId 举报者ID
     * @param targetType 目标类型 (1-笔记 2-评论 3-用户)
     * @param targetId 目标ID
     * @param reason 举报原因 (1-垃圾广告 2-涉黄 3-抄袭 4-其他)
     * @param description 详细描述
     * @return 成功返回true
     */
    boolean submitReport(Long reporterId, Integer targetType, Long targetId, Integer reason, String description);
    
    /**
     * 检查是否已举报
     * @param reporterId 举报者ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 是否已举报
     */
    boolean hasReported(Long reporterId, Integer targetType, Long targetId);
}