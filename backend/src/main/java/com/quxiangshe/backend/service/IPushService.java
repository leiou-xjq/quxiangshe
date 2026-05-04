package com.quxiangshe.backend.service;

public interface IPushService {
    void pushNotification(Long userId, String title, String message, String type, Long noteId);
}