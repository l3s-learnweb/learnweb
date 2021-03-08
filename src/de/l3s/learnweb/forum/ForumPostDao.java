package de.l3s.learnweb.forum;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(ForumPostDao.ForumPostMapper.class)
public interface ForumPostDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_forum_post WHERE post_id = ?")
    Optional<ForumPost> findById(int postId);

    @SqlQuery("SELECT * FROM lw_forum_post WHERE topic_id = ? ORDER BY created_at")
    List<ForumPost> findByTopicId(int topicId);

    @SqlQuery("SELECT * FROM lw_forum_post WHERE user_id = ? ORDER BY created_at DESC")
    List<ForumPost> findByUserId(int userId);

    @SqlQuery("SELECT COUNT(*) FROM lw_forum_post WHERE user_id = ?")
    int countByUserId(int userId);

    @SqlQuery("SELECT p.user_id, COUNT(*) as count FROM lw_forum_post p JOIN lw_forum_topic t USING (topic_id) WHERE group_id = ? GROUP BY p.user_id")
    @KeyColumn("user_id")
    @ValueColumn("count")
    Map<Integer, Integer> countPerUserByGroupId(int groupId);

    @SqlUpdate("DELETE FROM lw_forum_post WHERE post_id = ?")
    void delete(int postId);

    default void save(ForumPost post) {
        post.setUpdatedAt(SqlHelper.now());
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("post_id", SqlHelper.toNullable(post.getId()));
        params.put("deleted", post.isDeleted());
        params.put("topic_id", post.getTopicId());
        params.put("user_id", SqlHelper.toNullable(post.getUserId()));
        params.put("text", post.getText());
        params.put("edit_count", post.getEditCount());
        params.put("edit_user_id", SqlHelper.toNullable(post.getEditUserId()));
        params.put("category", post.getCategory());
        params.put("updated_at", post.getUpdatedAt());
        params.put("created_at", post.getCreatedAt());

        Optional<Integer> postId = SqlHelper.handleSave(getHandle(), "lw_forum_post", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        postId.ifPresent(post::setId);
    }

    class ForumPostMapper implements RowMapper<ForumPost> {
        @Override
        public ForumPost map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            ForumPost post = new ForumPost();
            post.setId(rs.getInt("post_id"));
            post.setTopicId(rs.getInt("topic_id"));
            post.setUserId(rs.getInt("user_id"));
            post.setText(rs.getString("text"));
            post.setEditCount(rs.getInt("edit_count"));
            post.setEditUserId(rs.getInt("edit_user_id"));
            post.setCategory(rs.getString("category"));
            post.setUpdatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
            post.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return post;
        }
    }
}
