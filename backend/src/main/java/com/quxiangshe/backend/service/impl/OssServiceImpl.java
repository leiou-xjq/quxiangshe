package com.quxiangshe.backend.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.VideoTranscodeMessage;
import com.quxiangshe.backend.service.IOssService;
import com.quxiangshe.backend.service.ImageService;
import com.quxiangshe.backend.util.VideoCoverExtractor;
import com.quxiangshe.backend.util.VideoTranscoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OSS服务实现类
 * 阿里云OSS对象存储 + 本地存储备用
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class OssServiceImpl implements IOssService {
    
    @Autowired
    @Lazy
    private OSS ossClient;
    
    @Autowired
    private ImageService imageService;
    
    @Autowired
    private VideoTranscoder videoTranscoder;
    
    @Autowired
    private VideoCoverExtractor videoCoverExtractor;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${oss.aliyun.endpoint:}")
    private String endpoint;
    
    @Value("${oss.aliyun.bucketName:}")
    private String bucketName;
    
    @Value("${oss.aliyun.enabled:false}")
    private boolean ossEnabled;
    
    @Value("${oss.local.upload-path:./uploads}")
    private String localUploadPath;
    
    @Value("${oss.image.compress-enabled:true}")
    private boolean compressEnabled;
    
    @Value("${oss.image.thumbnail-enabled:true}")
    private boolean thumbnailEnabled;
    
    @Value("${oss.image.quality:0.8}")
    private float imageQuality;
    
    private static final String OSS_BASE_URL = "https://";
    
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String[] ALLOWED_VIDEO_TYPES = {"mp4", "mov", "webm"};
    
    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        
        // 处理图片压缩
        byte[] imageData;
        String actualSuffix = suffix;
        
        if (compressEnabled && isImageFile(suffix)) {
            try {
                // 压缩图片
                imageData = imageService.compressImage(file, imageQuality);
                log.info("图片压缩完成，原始大小: {} bytes, 压缩后: {} bytes", 
                    file.getSize(), imageData.length);
            } catch (Exception e) {
                log.warn("图片压缩失败，使用原始图片", e);
                try {
                    imageData = file.getBytes();
                } catch (IOException ioException) {
                    throw new RuntimeException("读取图片文件失败", ioException);
                }
            }
        } else {
            try {
                imageData = file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("读取图片文件失败", e);
            }
        }
        
        // 生成文件名
        String newFileName = UUID.randomUUID().toString() + actualSuffix;
        
        if (ossEnabled && ossClient != null) {
            return uploadToOss(imageData, newFileName);
        } else {
            return uploadToLocal(imageData, newFileName);
        }
    }
    
    @Override
    public String uploadVideo(MultipartFile file) {
        return uploadVideo(file, null);
    }

    public String uploadVideo(MultipartFile file, Long noteId) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new RuntimeException("视频大小不能超过100MB");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ?
            originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : ".mp4";

        if (!isVideoFile(suffix)) {
            throw new RuntimeException("仅支持 mp4, mov, webm 格式的视频");
        }

        String tempFileName = UUID.randomUUID().toString() + suffix;

        try {
            Path tempDir = Files.createTempDirectory("video-upload-");
            Path tempFile = tempDir.resolve(tempFileName);

            file.transferTo(tempFile.toFile());
            log.info("视频已保存到临时文件: {}", tempFile);

            byte[] videoData = Files.readAllBytes(tempFile);

            String videoUrl;
            if (ossEnabled && ossClient != null) {
                videoUrl = uploadToOss(videoData, tempFileName);
            } else {
                videoUrl = uploadToLocal(videoData, tempFileName);
            }

            cleanupTempFiles(tempDir);

            if (noteId != null && rabbitTemplate != null && videoTranscoder.isAvailable()) {
                VideoTranscodeMessage msg = VideoTranscodeMessage.builder()
                        .noteId(noteId)
                        .originalUrl(videoUrl)
                        .targetFormat("mp4")
                        .targetWidth(1280)
                        .targetHeight(720)
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_EXCHANGE,
                        RabbitMQConfig.VIDEO_ROUTING_KEY, msg);
                log.info("视频转码任务已投递到MQ: noteId={}", noteId);
            }

            return videoUrl;

        } catch (IOException e) {
            throw new RuntimeException("处理视频文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 简单检查视频是否为 H.264 编码
     * 这里只是简单检查，不够精确
     */
    private boolean isH264Encoded(String filePath) {
        // 默认需要转码，让 FFmpeg 处理更保险
        return false;
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(Path tempDir) {
        try {
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.toFile().delete();
            log.info("临时文件已清理: {}", tempDir);
        } catch (Exception e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }
    }
    
    private boolean isVideoFile(String suffix) {
        if (suffix == null) return false;
        for (String type : ALLOWED_VIDEO_TYPES) {
            if (suffix.equals("." + type)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        if (ossEnabled && ossClient != null && fileUrl != null && fileUrl.contains(endpoint)) {
            deleteFromOss(fileUrl);
        } else if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
            deleteFromLocal(fileUrl);
        }
    }
    
    private String uploadToOss(byte[] data, String fileName) {
        try {
            ossClient.putObject(bucketName, fileName, new ByteArrayInputStream(data));
            String url = OSS_BASE_URL + bucketName + "." + endpoint + "/" + fileName;
            log.info("文件上传到OSS: {}", url);
            return url;
        } catch (Exception e) {
            log.error("OSS上传失败，使用本地存储", e);
            return uploadToLocal(data, fileName);
        }
    }
    
    private String uploadToLocal(byte[] data, String fileName) {
        try {
            Path uploadPath = Paths.get(localUploadPath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, data);
            
            String imageUrl = "/uploads/" + fileName;
            log.info("文件上传到本地: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    private void deleteFromOss(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            ossClient.deleteObject(bucketName, fileName);
            log.info("从OSS删除文件: {}", fileName);
        } catch (Exception e) {
            log.error("删除OSS文件失败", e);
        }
    }
    
    private void deleteFromLocal(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(localUploadPath, fileName);
            Files.deleteIfExists(filePath);
            log.info("从本地删除文件: {}", fileName);
        } catch (IOException e) {
            log.error("删除本地文件失败", e);
        }
    }
    
    private boolean isImageFile(String suffix) {
        if (suffix == null) return false;
        String lower = suffix.toLowerCase();
        return lower.equals(".jpg") || lower.equals(".jpeg") || 
               lower.equals(".png") || lower.equals(".gif") || 
               lower.equals(".webp") || lower.equals(".bmp");
    }
    
    @Override
    public String getSignedUrl(String fileUrl) {
        // 非 OSS URL 直接返回
        if (fileUrl == null || !fileUrl.contains(endpoint)) {
            return fileUrl;
        }
        
        // 本地存储文件直接返回
        if (fileUrl.startsWith("/uploads/")) {
            return fileUrl;
        }
        
        try {
            // 从 URL 中提取文件名
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            
            // 生成签名 URL，有效期 24 小时
            Date expiration = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName);
            request.setExpiration(expiration);
            
            URL signedUrl = ossClient.generatePresignedUrl(request);
            log.info("生成签名URL: {} -> {}", fileUrl, signedUrl.toString());
            
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("生成签名URL失败: {}", e.getMessage(), e);
            // 签名生成失败，返回原始 URL
            return fileUrl;
        }
    }
    
    @Override
    public Map<String, String> uploadVideoWithCover(MultipartFile file) {
        Map<String, String> result = new HashMap<>();

        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new RuntimeException("视频大小不能超过100MB");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") ?
            originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase() : ".mp4";

        if (!isVideoFile(suffix)) {
            throw new RuntimeException("仅支持 mp4, mov, webm 格式的视频");
        }

        String videoFileName = UUID.randomUUID().toString() + suffix;
        String coverFileName = UUID.randomUUID().toString() + ".jpg";

        try {
            Path tempDir = Files.createTempDirectory("video-cover-");
            Path videoFile = tempDir.resolve(videoFileName);
            Path coverFile = tempDir.resolve(coverFileName);

            file.transferTo(videoFile.toFile());
            log.info("视频已保存到临时文件: {}", videoFile);

            // 先上传视频
            byte[] videoData = Files.readAllBytes(videoFile);
            String videoUrl;
            if (ossEnabled && ossClient != null) {
                videoUrl = uploadToOss(videoData, videoFileName);
            } else {
                videoUrl = uploadToLocal(videoData, videoFileName);
            }
            result.put("videoUrl", videoUrl);

            // 提取封面
            if (videoCoverExtractor.isAvailable()) {
                String coverPath = videoCoverExtractor.extractCover(videoFile.toString(), coverFile.toString());
                if (coverPath != null && new File(coverPath).exists()) {
                    byte[] coverData = Files.readAllBytes(Paths.get(coverPath));
                    String coverUrl;
                    if (ossEnabled && ossClient != null) {
                        coverUrl = uploadToOss(coverData, coverFileName);
                    } else {
                        coverUrl = uploadToLocal(coverData, coverFileName);
                    }
                    result.put("coverUrl", coverUrl);
                    log.info("封面提取并上传成功: {}", coverUrl);
                }
            } else {
                log.warn("FFmpeg不可用，跳过封面提取");
            }

            cleanupTempFiles(tempDir);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("处理视频失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new RuntimeException("文件URL不能为空");
        }

        if (ossEnabled && ossClient != null && fileUrl.contains(endpoint)) {
            return downloadFromOss(fileUrl);
        } else if (fileUrl.startsWith("/uploads/")) {
            return downloadFromLocal(fileUrl);
        } else {
            throw new RuntimeException("不支持的文件URL类型: " + fileUrl);
        }
    }

    @Override
    public String uploadVideoData(byte[] data, String fileName) {
        if (data == null || data.length == 0) {
            throw new RuntimeException("视频数据不能为空");
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = UUID.randomUUID().toString() + ".mp4";
        }

        if (!fileName.toLowerCase().endsWith(".mp4")) {
            fileName = fileName + ".mp4";
        }

        if (ossEnabled && ossClient != null) {
            return uploadToOss(data, fileName);
        } else {
            return uploadToLocal(data, fileName);
        }
    }

    private byte[] downloadFromOss(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            OSS oss = ossClient;
            byte[] data = oss.getObject(bucketName, fileName).getObjectContent().readAllBytes();
            log.info("从OSS下载文件: {}, 大小: {} bytes", fileName, data.length);
            return data;
        } catch (Exception e) {
            throw new RuntimeException("从OSS下载文件失败: " + e.getMessage(), e);
        }
    }

    private byte[] downloadFromLocal(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(localUploadPath, fileName);
            byte[] data = Files.readAllBytes(filePath);
            log.info("从本地下载文件: {}, 大小: {} bytes", fileName, data.length);
            return data;
        } catch (Exception e) {
            throw new RuntimeException("从本地下载文件失败: " + e.getMessage(), e);
        }
    }
}
