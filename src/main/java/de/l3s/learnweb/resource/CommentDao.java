package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(CommentDao.CommentMapper.class)
public interface CommentDao extends SqlObject, Serializable {
    @SqlQuery("SELECT * FROM lw_comment WHERE comment_id = ?")
    Optional<Comment> findById(int commentId);

    @SqlQuery("SELECT c.* FROM lw_comment c JOIN lw_resource r USING(resource_id) WHERE c.user_id = ? AND r.deleted = 0")
    List<Comment> findByUserId(int userId);

    @SqlQuery("SELECT * FROM lw_comment JOIN lw_resource USING(resource_id) WHERE user_id IN (<userIds>) AND deleted = 0")
    List<Comment> findByUserIds(@BindList("userIds") Collection<Integer> userIds);

    @SqlQuery("SELECT * FROM lw_comment WHERE resource_id = ? ORDER BY created_at DESC")
    List<Comment> findByResourceId(int resourceId);

    @SqlUpdate("DELETE FROM lw_comment WHERE comment_id = ?")
    void delete(Comment comment);

    default void save(Comment comment) {
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("comment_id", SqlHelper.toNullable(comment.getId()));
        params.put("resource_id", SqlHelper.toNullable(comment.getResourceId()));
        params.put("user_id", SqlHelper.toNullable(comment.getUserId()));
        params.put("text", comment.getText());
        params.put("created_at", comment.getCreatedAt());

        Optional<Integer> commentId = SqlHelper.handleSave(getHandle(), "lw_comment", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (commentId.isPresent() && commentId.get() != 0) {
            comment.setId(commentId.get());
        }
    }

    class CommentMapper implements RowMapper<Comment> {
        @Override
        public Comment map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Comment comment = new Comment();
            comment.setId(rs.getInt("comment_id"));
            comment.setResourceId(rs.getInt("resource_id"));
            comment.setUserId(rs.getInt("user_id"));
            comment.setText(rs.getString("text"));
            comment.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return comment;
        }
    }
}
