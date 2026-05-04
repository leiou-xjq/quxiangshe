package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * 图片处理服务实现类
 * 使用 Java ImageIO 进行图片处理
 * 
 * @author 趣享社技术团队
 */
@Slf4j
@Service
public class ImageServiceImpl implements ImageService {
    
    private static final int DEFAULT_THUMBNAIL_WIDTH = 400;
    private static final int DEFAULT_THUMBNAIL_HEIGHT = 300;
    private static final int AVATAR_SIZE = 100;
    private static final float DEFAULT_QUALITY = 0.8f;
    
    @Override
    public byte[] compressImage(MultipartFile file, float quality) {
        try {
            InputStream inputStream = file.getInputStream();
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new RuntimeException("无法读取图片");
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = getImageFormat(file.getContentType());
            
            ImageIO.write(originalImage, format, outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("图片压缩失败", e);
            throw new RuntimeException("图片压缩失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] createThumbnail(MultipartFile file, int width, int height, float quality) {
        try {
            InputStream inputStream = file.getInputStream();
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new RuntimeException("无法读取图片");
            }
            
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            double scaleX = (double) width / originalWidth;
            double scaleY = (double) height / originalHeight;
            double scale = Math.min(scaleX, scaleY);
            
            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);
            
            BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = getImageFormat(file.getContentType());
            
            ImageIO.write(thumbnail, format, outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("生成缩略图失败", e);
            throw new RuntimeException("生成缩略图失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] convertToWebP(MultipartFile file, float quality) {
        try {
            InputStream inputStream = file.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            
            if (image == null) {
                throw new RuntimeException("无法读取图片");
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            writer.write(image);
            ios.close();
            writer.dispose();
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            log.error("转换为WebP失败", e);
            throw new RuntimeException("转换为WebP失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取图片格式
     */
    private String getImageFormat(String contentType) {
        if (contentType == null) {
            return "jpg";
        }
        
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }
    
    /**
     * 生成头像缩略图
     */
    public byte[] createAvatarThumbnail(MultipartFile file) {
        return createThumbnail(file, AVATAR_SIZE, AVATAR_SIZE, DEFAULT_QUALITY);
    }
    
    /**
     * 生成笔记封面缩略图
     */
    public byte[] createCoverThumbnail(MultipartFile file) {
        return createThumbnail(file, DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT, DEFAULT_QUALITY);
    }
}