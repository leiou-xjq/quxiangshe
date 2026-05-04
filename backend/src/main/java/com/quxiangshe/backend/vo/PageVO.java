package com.quxiangshe.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页响应VO
 * 
 * @author 趣享社技术团队
 */
@Data
@Schema(description = "分页响应")
public class PageVO<T> {
    
    @Schema(description = "数据列表")
    private List<T> records;
    
    @Schema(description = "是否有更多数据")
    private Boolean hasMore;
    
    @Schema(description = "下一页游标")
    private String nextCursor;
    
    public PageVO() {}
    
    public PageVO(List<T> records, Boolean hasMore, String nextCursor) {
        this.records = records;
        this.hasMore = hasMore;
        this.nextCursor = nextCursor;
    }
}