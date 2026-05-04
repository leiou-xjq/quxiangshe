package com.quxiangshe.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OSS对象存储服务接口
 * 
 * @author 趣享社技术团队
 */
public interface IOssService {
    
    /**
     * 上传图片
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadImage(MultipartFile file);
    
    /**
     * 上传视频
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadVideo(MultipartFile file);

    /**
     * 上传视频并触发异步转码
     * @param file 视频文件
     * @param noteId 笔记ID，用于转码完成后更新笔记
     * @return 文件访问URL
     */
    String uploadVideo(MultipartFile file, Long noteId);
    
    /**
     * 上传视频并自动提取封面
     * @param file 视频文件
     * @return 包含videoUrl和coverUrl的Map
     */
    Map<String, String> uploadVideoWithCover(MultipartFile file);
    
    /**
     * 删除文件
     * @param fileUrl 文件URL
     */
    void deleteFile(String fileUrl);
    
    /**
     * 获取文件的签名URL（用于私有 Bucket）
     * @param fileUrl 文件URL
     * @return 签名URL
     */
    String getSignedUrl(String fileUrl);

    /**
     * 下载文件内容
     * @param fileUrl 文件URL
     * @return 文件字节数组
     */
    byte[] downloadFile(String fileUrl);

    /**
     * 上传视频数据（用于转码后重新上传）
     * @param data 视频数据
     * @param fileName 文件名
     * @return 文件访问URL
     */
    String uploadVideoData(byte[] data, String fileName);
}
