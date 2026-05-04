package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 视频封面提取工具类
 * 使用 FFmpeg 提取视频第一帧作为封面
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class VideoCoverExtractor {
    
    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    
    @Value("${ffmpeg.timeout:60}")
    private int timeoutSeconds;
    
    /**
     * 从视频中提取第一帧作为封面
     * 
     * @param videoPath 视频路径（本地路径或OSS URL）
     * @param outputPath 输出封面路径
     * @return 封面文件路径，失败返回null
     */
    public String extractCover(String videoPath, String outputPath) {
        if (videoPath == null || videoPath.isEmpty()) {
            log.warn("视频路径为空，无法提取封面");
            return null;
        }
        
        // 如果是OSS URL，需要先下载到本地
        String localVideoPath = null;
        boolean tempFile = false;
        
        try {
            // 处理OSS URL
            if (videoPath.startsWith("http")) {
                localVideoPath = downloadToTemp(videoPath);
                if (localVideoPath == null) {
                    log.error("下载视频失败: {}", videoPath);
                    return null;
                }
                tempFile = true;
            } else {
                localVideoPath = videoPath;
            }
            
            // 构建FFmpeg命令
            // -ss 1: 从第1秒开始提取（避免黑屏）
            // -vframes 1: 只提取1帧
            // -q:v 2: 较高质量
            String[] command = {
                ffmpegPath,
                "-ss", "1",
                "-i", localVideoPath,
                "-vframes", "1",
                "-q:v", "2",
                "-y",  // 覆盖输出文件
                outputPath
            };
            
            log.info("开始提取视频封面: {} -> {}", videoPath, outputPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                log.error("提取封面超时，强制终止");
                process.destroyForcibly();
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0 && new File(outputPath).exists()) {
                log.info("封面提取成功: {}", outputPath);
                return outputPath;
            } else {
                log.error("封面提取失败，退出码: {}, 输出: {}", exitCode, output);
                return null;
            }
            
        } catch (Exception e) {
            log.error("提取封面异常: {}", e.getMessage(), e);
            return null;
        } finally {
            // 清理临时下载的文件
            if (tempFile && localVideoPath != null) {
                new File(localVideoPath).delete();
            }
        }
    }
    
    /**
     * 下载视频到临时文件
     */
    private String downloadToTemp(String url) {
        try {
            // 使用简单的方式 - 通过Java下载
            java.net.URL videoUrl = new java.net.URL(url);
            java.net.URLConnection conn = videoUrl.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            String suffix = getSuffix(url);
            File tempFile = File.createTempFile("video_", suffix);
            tempFile.deleteOnExit();
            
            try (java.io.InputStream is = conn.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("下载视频失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取文件后缀
     */
    private String getSuffix(String url) {
        int lastDot = url.lastIndexOf('.');
        if (lastDot > 0) {
            return url.substring(lastDot);
        }
        return ".mp4";
    }
    
    /**
     * 检查FFmpeg是否可用
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg不可用: {}", e.getMessage());
            return false;
        }
    }
}