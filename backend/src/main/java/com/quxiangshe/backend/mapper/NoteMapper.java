package com.quxiangshe.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quxiangshe.backend.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 笔记数据访问层接口，管理笔记（Note）的数据库操作。
 * <p>
 * 提供笔记列表查询、用户笔记查询、发现精彩Feed流（随机/游标分页）、
 * 热门榜单排序、关键词搜索、批量查询以及稳定随机数批量刷新等功能。
 * 所有公开查询均过滤 status=1（正常状态）的笔记。
 * </p>
 * 
 * @author 趣享社技术团队
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    
    /**
     * 查询笔记列表（带发布者信息）
     */
    @Select("""
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        ORDER BY n.created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<Note> selectNoteList(@Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 查询用户发布的笔记列表（只返回审核通过的笔记）
     */
    @Select("""
        <script>
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.user_id = #{userId}
        AND n.status = 1
        ORDER BY n.created_at DESC 
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Note> selectUserNotes(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 查询用户所有笔记的获赞总数
     */
    @Select("""
        SELECT COALESCE(SUM(like_count), 0) 
        FROM note 
        WHERE user_id = #{userId} AND status = 1
        """)
    Long selectSumLikeCountByUserId(@Param("userId") Long userId);
    
    /**
     * 发现精彩 - 首次加载（动态随机）
     * 每次刷新都使用不同的随机排序
     */
    @Select("""
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        ORDER BY RAND() 
        LIMIT #{size}
        """)
    List<Note> selectDiscoverNotes(@Param("size") int size);
    
    /**
     * 发现精彩 - 游标分页加载
     * 使用已查询到的ID列表进行分页，避免RAND()重复
     * @param excludeIds 排除的笔记ID（已展示的）
     * @param size 每页数量
     */
    @Select("""
        <script>
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        <if test="excludeIds != null and excludeIds.size() > 0">
        AND n.id NOT IN 
        <foreach collection="excludeIds" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </if>
        ORDER BY RAND() 
        LIMIT #{size}
        </script>
        """)
    List<Note> selectDiscoverNotesWithExclude(@Param("excludeIds") List<Long> excludeIds, @Param("size") int size);
    
    /**
     * 发现精彩 - 游标分页加载
     * 使用 stable_random + id 组合游标，避免翻页重复
     * @param cursorRandom stable_random 游标值
     * @param cursorId id 游标值
     * @param size 每页数量
     */
    @Select("""
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        AND (n.stable_random < #{cursorRandom} OR (n.stable_random = #{cursorRandom} AND n.id < #{cursorId}))
        ORDER BY n.stable_random DESC, n.id DESC
        LIMIT #{size}
        """)
    List<Note> selectDiscoverNotesByCursor(
        @Param("cursorRandom") BigDecimal cursorRandom,
        @Param("cursorId") Long cursorId,
        @Param("size") int size
    );
    
    /**
     * 热门 - 按热度排序查询笔记（游标分页）
     * 热度 = like_count * 3 + comment_count * 2 + favorite_count * 2 + view_count
     */
    @Select("""
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        ORDER BY (n.like_count * 3 + n.comment_count * 2 + n.favorite_count * 2 + n.view_count) DESC, n.created_at DESC
        LIMIT #{size}
        """)
    List<Note> selectPopularNotes(@Param("size") int size);

    /**
     * 搜索笔记 - 支持关键词、标签筛选、随机展示
     * keyword和tags都为空时，随机展示笔记
     */
    @Select("""
        <script>
        SELECT n.*, u.nickname, u.avatar 
        FROM note n 
        LEFT JOIN user u ON n.user_id = u.id 
        WHERE n.status = 1 
        <if test="keyword != null and keyword != ''">
        AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="tags != null and tags.size() > 0">
        AND (
            <foreach collection="tags" item="tag" separator=" OR ">
                n.tags LIKE CONCAT('%', #{tag}, '%')
            </foreach>
        )
        </if>
        ORDER BY 
            <choose>
                <when test="keyword == null or keyword == ''">RAND()</when>
                <otherwise>n.created_at DESC</otherwise>
            </choose>
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<Note> searchNotes(@Param("keyword") String keyword, @Param("tags") List<String> tags, @Param("limit") int limit, @Param("offset") int offset);
    
    /**
     * 批量查询笔记
     */
    @Select("""
        <script>
        SELECT * FROM note WHERE id IN 
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        </script>
         """)
    List<Note> selectByIds(@Param("ids") List<Long> ids);
    
    /**
     * 分批更新 stable_random
     * @param limit 每批更新数量
     * @return 本次更新的数量
     */
    @Update("""
        UPDATE note 
        SET stable_random = RAND() 
        WHERE id IN (
            SELECT id FROM (
                SELECT id FROM note 
                WHERE status = 1 AND stable_random IS NOT NULL 
                ORDER BY id 
                LIMIT #{limit}
            ) AS tmp
        )
        """)
    int updateStableRandomBatch(@Param("limit") int limit);
    
    /**
     * 统计有新变化（新增或热度更新）的笔记数量
     * @param lastSyncTime 上次同步时间（时间戳）
     * @return 变化的笔记数量
     */
    @Select("""
        SELECT COUNT(*) FROM note 
        WHERE status = 1 
        AND (
            created_at > FROM_UNIXTIME(#{lastSyncTime} / 1000)
            OR updated_at > FROM_UNIXTIME(#{lastSyncTime} / 1000)
            OR like_count > 0 
            OR comment_count > 0 
            OR favorite_count > 0 
            OR forward_count > 0
        )
        """)
    Long countChangedNotes(@Param("lastSyncTime") long lastSyncTime);
    
    /**
     * 查询有变化的笔记列表（新增或热度更新），用于Feed流同步。
     * @param lastSyncTime 上次同步时间（毫秒时间戳）
     * @return 变化的笔记列表（最多1000条）
     */
    @Select("""
        SELECT * FROM note 
        WHERE status = 1 
        AND (
            created_at > FROM_UNIXTIME(#{lastSyncTime} / 1000)
            OR updated_at > FROM_UNIXTIME(#{lastSyncTime} / 1000)
            OR like_count > 0 
            OR comment_count > 0 
            OR favorite_count > 0 
            OR forward_count > 0
        )
        ORDER BY id ASC
        LIMIT 1000
        """)
    List<Note> selectChangedNotes(@Param("lastSyncTime") long lastSyncTime);
    
    /**
     * 批量插入笔记
     */
    int batchInsert(@Param("list") List<Note> notes);
}
