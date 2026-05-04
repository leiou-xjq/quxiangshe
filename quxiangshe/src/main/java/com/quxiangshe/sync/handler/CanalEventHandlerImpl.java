package com.quxiangshe.sync.handler;

import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.note.service.NoteSearchService;
import com.quxiangshe.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Canal事件处理器实现
 * 处理数据变更，同步到ES索引
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanalEventHandlerImpl implements CanalEventHandler {

    private final NoteSearchService noteSearchService;
    private final SearchService searchService;
    private final NoteMapper noteMapper;

    @Override
    public void handleNoteInsert(Long noteId) {
        try {
            NoteEntity note = noteMapper.selectById(noteId);
            if (note != null && note.getDeleted() != 1) {
                noteSearchService.indexNote(note);
                log.info("笔记新增同步到ES完成: noteId={}", noteId);
            }
        } catch (Exception e) {
            log.error("笔记新增同步失败: noteId={}", noteId, e);
        }
    }

    @Override
    public void handleNoteUpdate(Long noteId) {
        try {
            NoteEntity note = noteMapper.selectById(noteId);
            if (note != null && note.getDeleted() != 1) {
                noteSearchService.updateNoteIndex(note);
                log.info("笔记更新同步到ES完成: noteId={}", noteId);
            } else if (note != null && note.getDeleted() == 1) {
                // 逻辑删除时删除索引
                noteSearchService.deleteNoteIndex(noteId);
                log.info("笔记逻辑删除，同步删除ES索引: noteId={}", noteId);
            }
        } catch (Exception e) {
            log.error("笔记更新同步失败: noteId={}", noteId, e);
        }
    }

    @Override
    public void handleNoteDelete(Long noteId) {
        try {
            noteSearchService.deleteNoteIndex(noteId);
            log.info("笔记删除同步到ES完成: noteId={}", noteId);
        } catch (Exception e) {
            log.error("笔记删除同步失败: noteId={}", noteId, e);
        }
    }

    @Override
    public void handleUserInsert(Long userId) {
        try {
            searchService.indexUser(userId);
            log.info("用户新增同步到ES完成: userId={}", userId);
        } catch (Exception e) {
            log.error("用户新增同步失败: userId={}", userId, e);
        }
    }

    @Override
    public void handleUserUpdate(Long userId) {
        try {
            searchService.indexUser(userId);
            log.info("用户更新同步到ES完成: userId={}", userId);
        } catch (Exception e) {
            log.error("用户更新同步失败: userId={}", userId, e);
        }
    }

    @Override
    public void handleUserDelete(Long userId) {
        try {
            searchService.deleteUserIndex(userId);
            log.info("用户删除同步到ES完成: userId={}", userId);
        } catch (Exception e) {
            log.error("用户删除同步失败: userId={}", userId, e);
        }
    }
}
