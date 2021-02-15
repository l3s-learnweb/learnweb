package de.l3s.learnweb.forum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

public interface ForumPostDao extends SqlObject {
    @SqlQuery("SELECT * FROM `lw_forum_post` WHERE post_id = ?")
    @RegisterRowMapper(ForumPostDao.ForumPostMapper.class)
    Optional<ForumPost> find(int postId);

    @SqlQuery("SELECT * FROM `lw_forum_post` WHERE topic_id = ? ORDER BY post_time")
    @RegisterRowMapper(ForumPostDao.ForumPostMapper.class)
    List<ForumPost> findAll(int topicId);

    @SqlQuery("SELECT * FROM `lw_forum_post` WHERE user_id = ? ORDER BY post_time DESC")
    @RegisterRowMapper(ForumPostDao.ForumPostMapper.class)
    List<ForumPost> findAllByUserId(int userId);

    @SqlQuery("SELECT COUNT(*) FROM `lw_forum_post` WHERE user_id = ?")
    int countByUserId(int userId);

    @SqlQuery("SELECT p.user_id, COUNT(*) as count FROM lw_forum_post p JOIN lw_forum_topic t USING (topic_id) WHERE group_id = ? GROUP BY p.user_id")
    @KeyColumn("user_id")
    @ValueColumn("count")
    Map<Integer, Integer> countPerUserByGroup(int groupId);

    @SqlUpdate("DELETE FROM lw_forum_post WHERE post_id = ?")
    void delete(int postId);

    default void insertOrUpdate(ForumPost post) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("post_id", post.getId() < 0 ? null : post.getId());
        params.put("deleted", post.isDeleted());
        params.put("topic_id", post.getTopicId());
        params.put("user_id", post.getUserId());
        params.put("text", post.getText());
        params.put("post_time", post.getDate());
        params.put("post_edit_time", post.getLastEditDate());
        params.put("post_edit_count", post.getEditCount());
        params.put("post_edit_user_id", post.getEditUserId());
        params.put("category", post.getCategory());

        try (Handle handle = getHandle()) {
            Optional<Integer> postId = SqlHelper.generateInsertQuery(handle, "lw_forum_post", params)
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

            postId.ifPresent(post::setId);
        }
    }

    class ForumPostMapper implements RowMapper<ForumPost> {
        @Override
        public ForumPost map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            ForumPost post = new ForumPost();
            post.setId(rs.getInt("post_id"));
            post.setTopicId(rs.getInt("topic_id"));
            post.setUserId(rs.getInt("user_id"));
            post.setText(rs.getString("text"));
            post.setDate(new Date(rs.getTimestamp("post_time").getTime()));
            post.setLastEditDate(new Date(rs.getTimestamp("post_edit_time").getTime()));
            post.setEditCount(rs.getInt("post_edit_count"));
            post.setEditUserId(rs.getInt("post_edit_user_id"));
            post.setCategory(rs.getString("category"));
            return post;
        }
    }
}
