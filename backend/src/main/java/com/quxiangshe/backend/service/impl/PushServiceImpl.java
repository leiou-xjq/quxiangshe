package com.quxiangshe.backend.service.impl;

import com.pusher.rest.Pusher;
import com.quxiangshe.backend.service.IPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements IPushService {

    private final Pusher pusher;

    @Override
    public void pushNotification(Long userId, String title, String message, String type, Long noteId) {
        if (userId == null) {
            log.warn("推送用户ID为空，跳过推送");
            return;
        }

        try {
            String channelName = "private-user-" + userId;

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("type", type);
            data.put("noteId", noteId);
            data.put("timestamp", System.currentTimeMillis());

            pusher.trigger(channelName, "notification", data);

            log.info("Pusher推送成功: userId={}, channel={}, type={}", userId, channelName, type);

        } catch (Exception e) {
            log.error("Pusher推送失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}