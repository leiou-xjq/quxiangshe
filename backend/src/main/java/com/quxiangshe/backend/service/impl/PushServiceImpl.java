package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.service.IPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushServiceImpl implements IPushService {

    @Override
    public void pushNotification(Long userId, String title, String message, String type, Long noteId) {
        if (userId == null) {
            log.warn("推送用户ID为空，跳过推送");
            return;
        }

        log.debug("推送功能已禁用 (Pusher依赖未配置): userId={}, title={}", userId, title);
    }
}