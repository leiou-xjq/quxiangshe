package com.quxiangshe.search.service.impl;

import com.quxiangshe.note.document.NoteDocument;
import com.quxiangshe.note.mapper.NoteMapper;
import com.quxiangshe.search.dto.SearchResponseDTO;
import com.quxiangshe.search.service.SearchService;
import com.quxiangshe.user.document.UserDocument;
import com.quxiangshe.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一搜索服务实现类
 * 使用Elasticsearch实现高性能搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;

    private static final String NOTE_INDEX = "t_note";
    private static final String USER_INDEX = "t_user";

    @Override
    public SearchResponseDTO search(String keyword, String type, Integer page, Integer size) {
        long startTime = System.currentTimeMillis();

        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        if (size > 50) {
            size = 50;
        }

        SearchResponseDTO response = new SearchResponseDTO();
        response.setPage(page);
        response.setSize(size);

        try {
            if (type == null || type.equals("all")) {
                SearchResponseDTO noteResult = searchNotes(keyword, null, page, size / 2 + 1);
                SearchResponseDTO userResult = searchUsers(keyword, page, size / 2 + 1);

                response.setNotes(noteResult.getNotes());
                response.setUsers(userResult.getUsers());
                response.setTotalCount(noteResult.getTotalCount() + userResult.getTotalCount());
                response.setHasMore(noteResult.getHasMore() || userResult.getHasMore());
            } else if (type.equals("note")) {
                return searchNotes(keyword, null, page, size);
            } else if (type.equals("user")) {
                return searchUsers(keyword, page, size);
            }
        } catch (Exception e) {
            log.error("统一搜索失败: keyword={}, type={}", keyword, type, e);
            response.setNotes(new ArrayList<>());
            response.setUsers(new ArrayList<>());
            response.setTotalCount(0L);
            response.setHasMore(false);
        }

        response.setCostTime(System.currentTimeMillis() - startTime);
        return response;
    }

    @Override
    public SearchResponseDTO searchNotes(String keyword, String category, Integer page, Integer size) {
        SearchResponseDTO response = new SearchResponseDTO();
        response.setType("note");

        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }

        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            
            // 基础过滤条件：状态正常且未删除
            boolQueryBuilder.filter(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                    .term(t -> t.field("status").value(1)));
            boolQueryBuilder.filter(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                    .term(t -> t.field("deleted").value(0)));

            // 关键词搜索（分词模糊匹配）
            if (keyword != null && !keyword.isEmpty()) {
                boolQueryBuilder.must(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("title^2", "content", "nickname", "username")
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        ));
            }

            // 分类过滤（精确匹配）
            if (category != null && !category.isEmpty()) {
                boolQueryBuilder.filter(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                        .term(t -> t.field("category").value(category)));
            }

            // 高亮配置
            HighlightParameters highlightParams = HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .withFragmentSize(100)
                    .withNumberOfFragments(3)
                    .build();

            List<HighlightField> highlightFields = List.of(
                    new HighlightField("title"),
                    new HighlightField("content")
            );

            Highlight highlight = new Highlight(highlightParams, highlightFields);

            // 构建查询
            NativeSearchQuery query = new NativeSearchQuery(boolQueryBuilder.build())
                    .setPageable(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")))
                    .setHighlightQuery(new HighlightQuery(highlight, NoteDocument.class));

            // 执行搜索
            SearchHits<NoteDocument> hits = elasticsearchOperations.search(query, NoteDocument.class);

            // 处理结果
            List<SearchResponseDTO.SearchNoteDTO> results = new ArrayList<>();
            for (var hit : hits.getSearchHits()) {
                NoteDocument doc = hit.getContent();
                if (doc != null) {
                    SearchResponseDTO.SearchNoteDTO dto = SearchResponseDTO.SearchNoteDTO.builder()
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
                            .createTime(doc.getCreateTime() != null ? doc.getCreateTime().toString() : null)
                            .build();

                    // 处理高亮
                    if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                        if (hit.getHighlightFields().containsKey("title")) {
                            dto.setHighlightTitle(hit.getHighlightFields().get("title").get(0));
                        }
                        if (hit.getHighlightFields().containsKey("content")) {
                            dto.setHighlightContent(hit.getHighlightFields().get("content").get(0));
                        }
                    }

                    results.add(dto);
                }
            }

            response.setNotes(results);
            response.setTotalCount(hits.getTotalHits());
            response.setHasMore(results.size() >= size);
            response.setPage(page);
            response.setSize(size);

            log.info("搜索笔记完成: keyword={}, category={}, resultCount={}", keyword, category, results.size());

        } catch (Exception e) {
            log.error("搜索笔记失败: keyword={}, category={}", keyword, category, e);
            response.setNotes(new ArrayList<>());
            response.setTotalCount(0L);
            response.setHasMore(false);
        }

        return response;
    }

    @Override
    public SearchResponseDTO searchUsers(String keyword, Integer page, Integer size) {
        SearchResponseDTO response = new SearchResponseDTO();
        response.setType("user");

        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0) {
            size = 20;
        }

        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 基础过滤条件：状态正常
            boolQueryBuilder.filter(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                    .term(t -> t.field("status").value(1)));

            // 关键词搜索（分词模糊匹配用户名和昵称）
            if (keyword != null && !keyword.isEmpty()) {
                boolQueryBuilder.must(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("username^2", "nickname^2", "bio")
                                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        ));
            }

            // 高亮配置
            HighlightParameters highlightParams = HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>")
                    .build();

            List<HighlightField> highlightFields = List.of(
                    new HighlightField("username"),
                    new HighlightField("nickname")
            );

            Highlight highlight = new Highlight(highlightParams, highlightFields);

            // 构建查询
            NativeSearchQuery query = new NativeSearchQuery(boolQueryBuilder.build())
                    .setPageable(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")))
                    .setHighlightQuery(new HighlightQuery(highlight, UserDocument.class));

            // 执行搜索
            SearchHits<UserDocument> hits = elasticsearchOperations.search(query, UserDocument.class);

            // 处理结果
            List<SearchResponseDTO.SearchUserDTO> results = new ArrayList<>();
            for (var hit : hits.getSearchHits()) {
                UserDocument doc = hit.getContent();
                if (doc != null) {
                    SearchResponseDTO.SearchUserDTO dto = SearchResponseDTO.SearchUserDTO.builder()
                            .userId(doc.getId())
                            .username(doc.getUsername())
                            .nickname(doc.getNickname())
                            .avatarUrl(doc.getAvatarUrl())
                            .bio(doc.getBio())
                            .createTime(doc.getCreateTime() != null ? doc.getCreateTime().toString() : null)
                            .build();

                    // 处理高亮
                    if (hit.getHighlightFields() != null && !hit.getHighlightFields().isEmpty()) {
                        if (hit.getHighlightFields().containsKey("username")) {
                            dto.setHighlightUsername(hit.getHighlightFields().get("username").get(0));
                        }
                        if (hit.getHighlightFields().containsKey("nickname")) {
                            dto.setHighlightNickname(hit.getHighlightFields().get("nickname").get(0));
                        }
                    }

                    results.add(dto);
                }
            }

            response.setUsers(results);
            response.setTotalCount(hits.getTotalHits());
            response.setHasMore(results.size() >= size);
            response.setPage(page);
            response.setSize(size);

            log.info("搜索用户完成: keyword={}, resultCount={}", keyword, results.size());

        } catch (Exception e) {
            log.error("搜索用户失败: keyword={}", keyword, e);
            response.setUsers(new ArrayList<>());
            response.setTotalCount(0L);
            response.setHasMore(false);
        }

        return response;
    }

    @Override
    public void indexUser(Long userId) {
        try {
            var userOpt = userMapper.selectById(userId);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                UserDocument doc = UserDocument.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .phone(user.getPhone())
                        .avatarUrl(user.getAvatarUrl())
                        .bio(user.getBio())
                        .status(user.getStatus())
                        .lastLoginTime(user.getLastLoginTime())
                        .createTime(user.getCreateTime())
                        .build();

                elasticsearchOperations.save(doc);
                log.info("用户索引成功: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("用户索引失败: userId={}", userId, e);
        }
    }

    @Override
    public void deleteUserIndex(Long userId) {
        try {
            elasticsearchOperations.delete(userId.toString(), UserDocument.class);
            log.info("删除用户索引: userId={}", userId);
        } catch (Exception e) {
            log.error("删除用户索引失败: userId={}", userId, e);
        }
    }

    @Override
    public void createIndexes() {
        log.info("ES索引由Spring Data自动管理");
    }
}
