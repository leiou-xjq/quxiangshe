package com.quxiangshe.sync.handler;

/**
 * Canal事件处理器接口
 */
public interface CanalEventHandler {

    /**
     * 处理笔记新增
     */
    void handleNoteInsert(Long noteId);

    /**
     * 处理笔记更新
     */
    void handleNoteUpdate(Long noteId);

    /**
     * 处理笔记删除
     */
    void handleNoteDelete(Long noteId);

    /**
     * 处理用户新增
     */
    void handleUserInsert(Long userId);

    /**
     * 处理用户更新
     */
    void handleUserUpdate(Long userId);

    /**
     * 处理用户删除
     */
    void handleUserDelete(Long userId);
}
