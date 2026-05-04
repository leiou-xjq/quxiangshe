package com.quxiangshe.note.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quxiangshe.note.document.NoteDocument;
import com.quxiangshe.note.dto.NoteResponseDTO;
import com.quxiangshe.note.entity.NoteEntity;
import com.quxiangshe.note.repository.NoteElasticsearchRepository;
import com.quxiangshe.note.service.NoteSearchService;
import com.quxiangshe.auth.entity.UserEntity;
import com.quxiangshe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记搜索服务实现类
 * 使用Spring Data Elasticsearch进行全文检索
 * 支持高亮、分词模糊匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSearchServiceImpl implements NoteSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NoteElasticsearchRepository elasticsearchRepository;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    @Override
    public void indexNote(NoteEntity note) {
        try {
            NoteDocument document = convertToDocument(note);
            elasticsearchRepository.save(document);
            log.info("笔记索引成功: noteId={}", note.getId());
        } catch (Exception e) {
            log.error("笔记索引失败: noteId={}", note.getId(), e);
            throw e;
        }
    }

    @Override
    public void batchIndexNotes(List<NoteEntity> notes) {
        try {
            List<NoteDocument> documents = new ArrayList<>();
            for (NoteEntity note : notes) {
                documents.add(convertToDocument(note));
            }
            elasticsearchRepository.saveAll(documents);
            log.info("批量索引笔记完成: {} 条", documents.size());
        } catch (Exception e) {
            log.error("批量索引笔记失败", e);
            throw e;
        }
    }

    @Override
    public void deleteNoteIndex(Long noteId) {
        try {
            elasticsearchRepository.deleteById(noteId);
            log.info("删除笔记索引: noteId={}", noteId);
        } catch (Exception e) {
            log.error("删除笔记索引失败: noteId={}", noteId, e);
        }
    }

    @Override
    public void updateNoteIndex(NoteEntity note) {
        try {
            NoteDocument document = convertToDocument(note);
            elasticsearchRepository.save(document);
            log.info("更新笔记索引: noteId={}", note.getId());
        } catch (Exception e) {
            log.error("更新笔记索引失败: noteId={}", note.getId(), e);
            throw e;
        }
    }

    @Override
    public List<NoteResponseDTO> searchNotes(String keyword, String category, Long userId, int page, int size) {
        try {
            Criteria criteria = new Criteria("status").is(1)
                    .and("deleted").is(0);

            if (keyword != null && !keyword.isEmpty()) {
                criteria = criteria.and(new Criteria("title").matches(keyword)
                        .or(new Criteria("content").matches(keyword)));
            }

            if (category != null && !category.isEmpty()) {
                criteria = criteria.and("category").is(category);
            }

            if (userId != null) {
                criteria = criteria.and("userId").is(userId);
            }

            Query query = new CriteriaQuery(criteria)
                    .setPageable(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));

            SearchHits<NoteDocument> hits = elasticsearchOperations.search(query, NoteDocument.class);

            List<NoteResponseDTO> results = new ArrayList<>();
            for (var hit : hits.getSearchHits()) {
                NoteDocument doc = hit.getContent();
                if (doc != null) {
                    NoteResponseDTO dto = convertToDTO(doc);
                    results.add(dto);
                }
            }

            log.info("搜索笔记完成: keyword={}, category={}, userId={}, resultCount={}", 
                    keyword, category, userId, results.size());

            return results;
        } catch (Exception e) {
            log.error("搜索笔记失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void createIndex() {
        log.info("ES索引由Spring Data自动管理");
    }

    private NoteDocument convertToDocument(NoteEntity note) {
        List<String> tagsList = null;
        if (note.getTags() != null) {
            try {
                tagsList = objectMapper.readValue(note.getTags(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("解析标签失败: {}", note.getTags());
            }
        }

        String nickname = "";
        String username = "";
        String avatarUrl = "";
        try {
            var userOpt = userMapper.selectById(note.getUserId());
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                nickname = user.getNickname() != null ? user.getNickname() : "";
                username = user.getUsername() != null ? user.getUsername() : "";
                avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
            }
        } catch (Exception e) {
            log.warn("查询用户信息失败: userId={}", note.getUserId());
        }

        return NoteDocument.builder()
                .id(note.getId())
                .userId(note.getUserId())
                .nickname(nickname)
                .username(username)
                .avatarUrl(avatarUrl)
                .title(note.getTitle())
                .content(note.getContent())
                .coverImage(note.getCoverImage())
                .category(note.getCategory())
                .tags(tagsList)
                .likeCount(note.getLikeCount())
                .commentCount(note.getCommentCount())
                .collectCount(note.getCollectCount())
                .viewCount(note.getViewCount())
                .status(note.getStatus())
                .deleted(note.getDeleted())
                .createTime(note.getCreateTime())
                .build();
    }

    private NoteResponseDTO convertToDTO(NoteDocument doc) {
        return NoteResponseDTO.builder()
                .noteId(doc.getId())
                .userId(doc.getUserId())
                .username(doc.getUsername())
                .nickname(doc.getNickname())
                .avatarUrl(doc.getAvatarUrl())
                .title(doc.getTitle())
                .content(doc.getContent())
                .coverImage(doc.getCoverImage())
                .category(doc.getCategory())
                .tags(doc.getTags())
                .likeCount(doc.getLikeCount())
                .commentCount(doc.getCommentCount())
                .collectCount(doc.getCollectCount())
                .viewCount(doc.getViewCount())
                .isLiked(false)
                .isCollected(false)
                .createTime(doc.getCreateTime() != null ? doc.getCreateTime().toString() : null)
                .build();
    }
}
