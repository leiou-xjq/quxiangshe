package com.quxiangshe.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quxiangshe.backend.entity.Report;
import com.quxiangshe.backend.mapper.ReportMapper;
import com.quxiangshe.backend.service.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 举报服务实现类
 * 
 * @author 趣享社技术团队
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {
    
    private final ReportMapper reportMapper;
    
    @Override
    public boolean submitReport(Long reporterId, Integer targetType, Long targetId, Integer reason, String description) {
        // 检查是否已举报
        if (hasReported(reporterId, targetType, targetId)) {
            throw new RuntimeException("您已经举报过此内容");
        }
        
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus(0); // 待处理
        
        return reportMapper.insert(report) > 0;
    }
    
    @Override
    public boolean hasReported(Long reporterId, Integer targetType, Long targetId) {
        QueryWrapper<Report> wrapper = new QueryWrapper<>();
        wrapper.eq("reporter_id", reporterId)
               .eq("target_type", targetType)
               .eq("target_id", targetId);
        
        return reportMapper.selectCount(wrapper) > 0;
    }
}