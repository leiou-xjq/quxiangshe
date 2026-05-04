package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报记录Mapper接口
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}