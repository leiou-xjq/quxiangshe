package com.quxiangshe.backend.consumer;

import com.quxiangshe.backend.config.RabbitMQConfig;
import com.quxiangshe.backend.dto.VideoTranscodeMessage;
import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.mapper.NoteMapper;
import com.quxiangshe.backend.service.IOssService;
import com.quxiangshe.backend.util.VideoTranscoder;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTranscodeConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    private final NoteMapper noteMapper;
    private final IOssService ossService;
    private final VideoTranscoder videoTranscoder;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_TRANSCODE_QUEUE)
    public void consumeTranscode(VideoTranscodeMessage message, Message amqpMessage, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到视频转码消息: noteId={}, originalUrl={}", message.getNoteId(), message.getOriginalUrl());

        int retryCount = getRetryCount(amqpMessage);

        try {
            String transcodedUrl = transcodeVideo(message);

            if (message.getNoteId() != null && transcodedUrl != null) {
                updateNoteVideoUrl(message.getNoteId(), transcodedUrl);
            }

            channel.basicAck(deliveryTag, false);
            log.info("视频转码成功: noteId={}, newUrl={}", message.getNoteId(), transcodedUrl);

        } catch (Exception e) {
            log.error("视频转码失败: noteId={}, retryCount={}, error={}",
                    message.getNoteId(), retryCount, e.getMessage(), e);

            handleFailure(channel, amqpMessage, deliveryTag, retryCount, e);
        }
    }

    private String transcodeVideo(VideoTranscodeMessage message) throws Exception {
        log.info("开始下载原视频: url={}", message.getOriginalUrl());
        byte[] originalVideo = ossService.downloadFile(message.getOriginalUrl());

        Path tempDir = Files.createTempDirectory("video-transcode-");
        Path tempInput = tempDir.resolve("input.mp4");
        Path tempOutput = tempDir.resolve("output.mp4");

        try {
            Files.write(tempInput, originalVideo);

            log.info("开始转码视频: input={}, targetFormat={}", tempInput, message.getTargetFormat());
            boolean success = videoTranscoder.transcode(tempInput.toString(), tempOutput.toString());

            if (!success) {
                throw new RuntimeException("视频转码失败");
            }

            byte[] transcodedVideo = Files.readAllBytes(tempOutput);
            log.info("转码完成，转码后大小: {} bytes", transcodedVideo.length);

            String newUrl = ossService.uploadVideoData(transcodedVideo,
                    "transcoded_" + System.currentTimeMillis() + ".mp4");

            return newUrl;

        } finally {
            cleanupTempFiles(tempDir);
        }
    }

    private void updateNoteVideoUrl(Long noteId, String newUrl) {
        Note note = noteMapper.selectById(noteId);
        if (note != null) {
            note.setVideo(newUrl);
            noteMapper.updateById(note);
            log.info("更新笔记视频URL: noteId={}, newUrl={}", noteId, newUrl);
        } else {
            log.warn("笔记不存在: noteId={}", noteId);
        }
    }

    private void cleanupTempFiles(Path tempDir) {
        try {
            File[] files = tempDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.toFile().delete();
        } catch (Exception e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }
    }

    private void handleFailure(Channel channel, Message amqpMessage, long deliveryTag,
                               int retryCount, Exception e) {
        try {
            if (retryCount < MAX_RETRY_COUNT) {
                String retryHeader = String.valueOf(retryCount + 1);
                amqpMessage.getMessageProperties().setHeader("x-retry-count", retryHeader);

                channel.basicNack(deliveryTag, false, true);
                log.warn("视频转码重试: retryCount={}", retryCount + 1);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.error("视频转码超过最大重试次数，丢弃消息: retryCount={}", retryCount);
            }
        } catch (Exception ackEx) {
            log.error("确认消息失败时出现异常: {}", ackEx.getMessage(), ackEx);
        }
    }

    private int getRetryCount(Message message) {
        Object retryHeader = message.getMessageProperties().getHeader("x-retry-count");
        if (retryHeader != null) {
            try {
                return Integer.parseInt(retryHeader.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}