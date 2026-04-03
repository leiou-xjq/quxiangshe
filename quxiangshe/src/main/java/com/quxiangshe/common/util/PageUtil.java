package com.quxiangshe.common.util;

import lombok.experimental.UtilityClass;

/**
 * 分页工具类
 */
@UtilityClass
public class PageUtil {

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页数量
     */
    public static final int DEFAULT_SIZE = 20;

    /**
     * 最大每页数量
     */
    public static final int MAX_SIZE = 50;

    /**
     * 校验并修正分页参数
     */
    public static int correctPage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * 校验并修正每页数量
     */
    public static int correctSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            return MAX_SIZE;
        }
        return size;
    }

    /**
     * 计算偏移量
     */
    public static int calculateOffset(int page, int size) {
        return (correctPage(page) - 1) * correctSize(size);
    }

    /**
     * 计算游标分页的偏移量
     * 游标分页不需要计算offset，使用lastId和lastTime进行定位
     */
    public static long calculateCursorOffset(Long lastId, Integer size) {
        return lastId == null ? 0 : lastId;
    }
}
