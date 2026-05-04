package com.quxiangshe.backend.scheduler;

import com.quxiangshe.backend.service.IPrivateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageArchiveScheduler {
    
    private final IPrivateMessageService privateMessageService;
    
    @Value("${message.archive.days:30}")
    private int archiveDays;
    
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