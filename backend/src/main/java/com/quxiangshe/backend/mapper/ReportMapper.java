package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报记录数据访问层接口，管理用户举报记录（Report）的数据库操作。
 * <p>
 * 继承MyBatis-Plus BaseMapper，提供基础CRUD功能。
 * 举报记录涵盖笔记、评论、用户三种目标的举报信息，
 * 支持状态流转（待处理→已处理/已驳回）。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}