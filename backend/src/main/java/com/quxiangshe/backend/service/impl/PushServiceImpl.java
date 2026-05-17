package com.quxiangshe.backend.service.impl;

import com.quxiangshe.backend.config.ChatWebSocketHandler;
import com.quxiangshe.backend.service.IPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息推送服务实现类
 *
 * <p>核心职责：
 * <ul>
 *   <li>通过 WebSocket 向指定用户实时推送通知消息</li>
 *   <li>封装 WebSocket Session 管理，按用户ID投递消息</li>
 * </ul>
 *
 * <p>所属业务模块：消息推送管理
 *
 * @author 趣享社技术团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements IPushService {

    private final ChatWebSocketHandler chatWebSocketHandler;

    /**
     * 通过 WebSocket 向指定用户推送通知消息
     *
     * <p>若用户未在线（无 WebSocket 连接），消息将静默丢弃。
     *
     * @param userId  目标用户ID，为 null 时跳过推送
     * @param title   通知标题
     * @param message 通知内容
     * @param type    通知类型（用于前端路由跳转）
     * @param noteId  关联笔记ID，为 null 时传空字符串
     */
    @Override
    public void pushNotification(Long userId, String title, String message, String type, Long noteId) {
        // 用户ID为空时无法确定推送目标，静默跳过
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
