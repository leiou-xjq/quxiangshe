package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.config.ChatWebSocketHandler;
import com.quxiangshe.backend.service.IPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements IPushService {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void pushNotification(Long userId, String title, String message, String type, Long noteId) {
        if (userId == null) {
            log.warn("推送用户ID为空，跳过推送");
            return;
        }

        chatWebSocketHandler.sendNotification(userId, "notification", Map.of(
                "title", title,
                "message", message,
                "notificationType", type,
                "noteId", noteId != null ? noteId : ""
        ));
    }
}
