package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * 视频转码工具类
 * 使用 FFmpeg 将视频转换为浏览器兼容的 H.264 编码
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Component
public class VideoTranscoder {
    
    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    
    @Value("${ffmpeg.timeout:300}")
    private int timeoutSeconds;
    
    /**
     * 转码视频为浏览器兼容格式
     * @param inputPath 输入视频路径（本地或OSS临时路径）
     * @param outputPath 输出视频路径
     * @return 转码是否成功
     */
    public boolean transcode(String inputPath, String outputPath) {
        if (inputPath == null || outputPath == null) {
            log.warn("转码参数为空，跳过");
            return false;
        }
        
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            log.warn("输入文件不存在: {}", inputPath);
            return false;
        }
        
        try {
            // 构建 FFmpeg 转码命令
            // -c:v libx264: 使用 H.264 编码
            // -c:a aac: 使用 AAC 音频编码
            // -movflags +faststart: 允许视频流在文件完全下载前开始播放
            // -preset medium: 中等编码速度，平衡质量和大小
            // -crf 23: 控制质量，23是默认值，数值越小质量越高
            String[] command = {
                ffmpegPath,
                "-i", inputPath,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-movflags", "+faststart",
                "-preset", "medium",
                "-crf", "23",
                "-y",  // 覆盖输出文件
                outputPath
            };
            
            log.info("开始转码: {} -> {}", inputPath, outputPath);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取转码输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 等待转码完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                log.error("转码超时，强制终止");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("转码成功: {}", outputPath);
                return true;
            } else {
                log.error("转码失败，退出码: {}, 输出: {}", exitCode, output);
                return false;
            }
            
        } catch (Exception e) {
            log.error("转码异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查 FFmpeg 是否可用
     * @return 是否可用
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("FFmpeg 可用");
                return true;
            }
        } catch (Exception e) {
            log.warn("FFmpeg 不可用: {}", e.getMessage());
        }
        return false;
    }
}