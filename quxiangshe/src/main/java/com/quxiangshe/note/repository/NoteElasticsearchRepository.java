package com.quxiangshe.note.repository;

import com.quxiangshe.note.document.NoteDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 笔记ES Repository
 */
@Repository
public interface NoteElasticsearchRepository extends ElasticsearchRepository<NoteDocument, Long> {
}
