package com.quxiangshe.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.comment.entity.SensitiveWordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词Mapper接口
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWordEntity> {
}
