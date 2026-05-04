package com.quxiangshe.backend;

import com.quxiangshe.backend.entity.Note;
import com.quxiangshe.backend.mapper.NoteMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@SpringBootTest
public class CommentTestDataGenerator {

    @Autowired
    private NoteMapper noteMapper;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static final String[] COMMENT_CONTENTS = {
        "写得真好！", "太棒了", "支持一下", "顶顶顶", "赞赞赞",
        "哈哈哈", "学到了", "收藏了", "转发", "打卡",
        "写得不错", "很有道理", "受教了", "666", "厉害",
        "真香", "绝了", "可以", "ok", "收到",
        "学习", "加油", "冲", "棒", "好",
        "不错", "点赞", "必须", "好看", "爱了"
    };

    private static final String[] REPLY_CONTENTS = {
        "回复 @用户：", "谢谢", "同感", "哈哈", "确实",
        "对的", "我也是", "赞", "好观点", "明白了",
        "有道理", "厉害", "哈哈笑死", "绝了", "太真实"
    };

    @Test
    public void generateCommentTestData() throws Exception {
        log.info("========== 开始生成评论测试数据 ==========");
        long startTime = System.currentTimeMillis();

        List<Note> allNotes = noteMapper.selectList(null);
        if (allNotes.size() < 3) {
            log.error("笔记数量不足，需要至少3篇笔记");
            return;
        }

        Collections.shuffle(allNotes);
        List<Note> selectedNotes = allNotes.subList(0, 3);

        int[] commentCounts = {1000, 10000, 100000};

        for (int i = 0; i < 3; i++) {
            Note note = selectedNotes.get(i);
            int count = commentCounts[i];
            generateCommentsForNote(note, count);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("========== 评论测试数据生成完成，耗时{}ms ==========", elapsed);
    }

    private void generateCommentsForNote(Note note, int totalCount) throws Exception {
        log.info("开始为笔记{}生成{}条评论...", note.getId(), totalCount);
        long noteStartTime = System.currentTimeMillis();

        int rootCount = (int) (totalCount * 0.30);
        int childCount = (int) (totalCount * 0.35);
        int grandchildCount = totalCount - rootCount - childCount;
        int insertedCount = 0;

        log.info("  根评论: {}条, 子评论: {}条, 孙评论: {}条", rootCount, childCount, grandchildCount);

        Properties props = new Properties();
        props.setProperty("user", "root");
        props.setProperty("password", "123456");
        props.setProperty("allowMultiQueries", "true");

        String url = "jdbc:mysql://localhost:3306/quxiangshe?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true";
        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            long nextAutoBefore = getAutoIncrement(conn, "note_comment");
            int rootInserted = insertRootComments(conn, note.getId(), rootCount);
            long nextAutoAfter = getAutoIncrement(conn, "note_comment");
            insertedCount = rootInserted;
            long rootIdStart = nextAutoBefore;
            long rootIdEnd = nextAutoBefore + rootInserted - 1;
            log.info("  根评论: 插入 {} 条, ID范围: {} ~ {}", rootInserted, rootIdStart, rootIdEnd);

            List<Long> rootIds = new ArrayList<>();
            for (long id = rootIdStart; id <= rootIdEnd; id++) rootIds.add(id);

            if (rootIds.isEmpty()) {
                log.error("  根评论插入失败，跳过该笔记");
                return;
            }

            insertChildComments(conn, note.getId(), rootIds, childCount);
            log.info("  子评论: 插入 {} 条", childCount);

            List<Long> childIds = new ArrayList<>();
            long childIdStartBefore = nextAutoAfter;
            long childIdEnd = childIdStartBefore + childCount - 1;
            for (long id = childIdStartBefore; id <= childIdEnd; id++) childIds.add(id);

            if (childIds.isEmpty()) {
                log.warn("  子评论插入失败，孙评论跳过");
            } else {
                insertGrandchildComments(conn, note.getId(), rootIds, childIds, grandchildCount);
                log.info("  孙评论: 插入 {} 条", grandchildCount);
            }

            conn.commit();
        }

        note.setCommentCount(totalCount);
        noteMapper.updateById(note);

        long noteElapsed = System.currentTimeMillis() - noteStartTime;
        log.info("  笔记{}评论生成完成，实际插入 {} 条，耗时 {}ms", note.getId(), insertedCount + childCount + grandchildCount, noteElapsed);
    }

    private long getAutoIncrement(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT AUTO_INCREMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + table + "'")) {
            if (rs.next()) return rs.getLong(1);
            throw new SQLException("Cannot get AUTO_INCREMENT for " + table);
        }
    }

    private int insertRootComments(Connection conn, long noteId, int count) throws SQLException {
        String sql = "INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at) VALUES (?,?,0,0,?,?,1,?,?)";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int batchSize = 1000;
        int totalInserted = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setLong(1, noteId);
                ps.setLong(2, random.nextLong(1, 201));
                ps.setString(3, COMMENT_CONTENTS[random.nextInt(COMMENT_CONTENTS.length)] + " " + System.currentTimeMillis() + i);
                ps.setInt(4, random.nextInt(2000));
                ps.setString(5, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.setString(6, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.addBatch();
                totalInserted++;
                if (totalInserted % batchSize == 0) {
                    ps.executeBatch();
                    log.info("  根评论: 已插入 {} 条", totalInserted);
                }
            }
            ps.executeBatch();
            return totalInserted;
        }
    }

    private void insertChildComments(Connection conn, long noteId, List<Long> rootIds, int count) throws SQLException {
        String sql = "INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at) VALUES (?,?,?,?,?,?,1,?,?)";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int batchSize = 1000;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                long parentId = rootIds.get(random.nextInt(rootIds.size()));
                ps.setLong(1, noteId);
                ps.setLong(2, random.nextLong(1, 201));
                ps.setLong(3, parentId);
                ps.setLong(4, parentId);
                ps.setString(5, REPLY_CONTENTS[random.nextInt(REPLY_CONTENTS.length)] + " " + System.currentTimeMillis() + i);
                ps.setInt(6, random.nextInt(1500));
                ps.setString(7, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.setString(8, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.addBatch();
                if ((i + 1) % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void insertGrandchildComments(Connection conn, long noteId, List<Long> rootIds, List<Long> childIds, int count) throws SQLException {
        String sql = "INSERT INTO note_comment (note_id, user_id, parent_id, root_id, content, like_count, status, created_at, updated_at) VALUES (?,?,?,?,?,?,1,?,?)";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int batchSize = 1000;
        int totalInserted = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                long parentId = childIds.get(random.nextInt(childIds.size()));
                long rootId = rootIds.get(random.nextInt(rootIds.size()));
                ps.setLong(1, noteId);
                ps.setLong(2, random.nextLong(1, 201));
                ps.setLong(3, parentId);
                ps.setLong(4, rootId);
                ps.setString(5, "追问 " + System.currentTimeMillis() + " " + i);
                ps.setInt(6, random.nextInt(1000));
                ps.setString(7, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.setString(8, LocalDateTime.now().minusDays(random.nextInt(30)).format(dtf));
                ps.addBatch();
                totalInserted++;
                if (totalInserted % batchSize == 0) {
                    ps.executeBatch();
                    log.info("  孙评论: 已插入 {} 条", totalInserted);
                }
            }
            ps.executeBatch();
            log.info("  孙评论: 已插入 {} 条", totalInserted);
        }
    }

    @Test
    public void clearAllComments() {
        log.info("清空所有评论数据...");
        com.quxiangshe.backend.mapper.NoteCommentMapper commentMapper = 
            com.quxiangshe.backend.mapper.NoteCommentMapper.class.cast(
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    new org.springframework.beans.factory.support.DefaultListableBeanFactory(), "getBean"
                )
            );
    }

    @Test
    public void clearCommentsViaNoteMapper() {
    }
}