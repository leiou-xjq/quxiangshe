package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.IPrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 私信消息归档定时任务
 * <p>每日凌晨3点执行，将超过指定天数的私信消息从主表归档到历史表，
 * 释放主表存储空间，同时保证历史数据可追溯。
 * <p>归档天数通过配置项 {@code message.archive.days} 指定，默认30天。
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveScheduler {
    
    private final IPrivateMessageService privateMessageService;
    
    /** 归档阈值天数，默认30天，可通过配置覆盖 */
    @Value("${message.archive.days:30}")
    private int archiveDays;
    
    /**
     * 每日凌晨3点归档过期私信
     * <p>将发送时间超过archiveDays天的消息迁移至归档存储
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveOldMessages() {
        log.info("开始归档超过{}天的私信消息", archiveDays);
        try {
            privateMessageService.archiveOldMessages(archiveDays);
            log.info("私信消息归档完成");
        } catch (Exception e) {
            log.error("私信消息归档失败", e);
        }
    }
}