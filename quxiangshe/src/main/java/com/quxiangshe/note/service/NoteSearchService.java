package com.quxiangshe.note.service;

import com.quxiangshe.note.document.NoteDocument;
import com.quxiangshe.note.dto.NoteResponseDTO;
import com.quxiangshe.note.entity.NoteEntity;

import java.util.List;

/**
 * 笔记搜索服务接口
 */
public interface NoteSearchService {

    /**
     * 索引笔记文档
     *
     * @param note 笔记实体
     */
    void indexNote(NoteEntity note);

    /**
     * 批量索引笔记
     *
     * @param notes 笔记列表
     */
    void batchIndexNotes(List<NoteEntity> notes);

    /**
     * 删除笔记索引
     *
     * @param noteId 笔记ID
     */
    void deleteNoteIndex(Long noteId);

    /**
     * 更新笔记索引
     *
     * @param note 笔记实体
     */
    void updateNoteIndex(NoteEntity note);

    /**
     * 搜索笔记
     *
     * @param keyword 关键词
     * @param category 分类
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 笔记列表
     */
    List<NoteResponseDTO> searchNotes(String keyword, String category, Long userId, int page, int size);

    /**
     * 创建索引
     */
    void createIndex();
}
