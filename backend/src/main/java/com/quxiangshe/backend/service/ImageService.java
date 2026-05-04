package com.quxiangshe.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片处理服务接口
 * 
 * @author 趣享社技术团队
 */
public interface ImageService {
    
    /**
     * 压缩图片
     * @param file 原始图片文件
     * @param quality 质量 (0.0-1.0)
     * @return 压缩后的字节数组
     */
    byte[] compressImage(MultipartFile file, float quality);
    
    /**
     * 压缩图片并生成缩略图
     * @param file 原始图片文件
     * @param width 目标宽度
     * @param height 目标高度
     * @param quality 质量 (0.0-1.0)
     * @return 缩略图字节数组
     */
    byte[] createThumbnail(MultipartFile file, int width, int height, float quality);
    
    /**
     * 转换为WebP格式
     * @param file 原始图片文件
     * @param quality 质量 (0.0-1.0)
     * @return WebP格式的字节数组
     */
    byte[] convertToWebP(MultipartFile file, float quality);
}