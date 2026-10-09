package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(AnnotationDao.AnnotationMapper.class)
public interface AnnotationDao extends SqlObject, Serializable {

    @SqlQuery("SELECT a.* FROM lw_transcript_log a JOIN lw_resource USING(resource_id) WHERE user_id IN(<userIds>) and deleted = 0 ORDER BY user_id, created_at DESC")
    List<Annotation> findLogsByUserIds(@BindList("userIds") Collection<Integer> userIds);

    @SqlQuery("SELECT a.* FROM lw_resource_annotation a WHERE resource_id = ?")
    List<Annotation> findAllByResourceId(int resourceId);

    default void save(Annotation annotation) {
        if (annotation.getCreatedAt() == null) {
            annotation.setCreatedAt(Instant.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("annotation_id", annotation.getAnnotationId());
        params.put("resource_id", annotation.getResourceId());
        params.put("user_id", annotation.getUserId());
        params.put("action", SqlHelper.toNullable(annotation.getAction()));
        params.put("selection", SqlHelper.toNullable(annotation.getSelection()));
        params.put("annotation", SqlHelper.toNullable(annotation.getAnnotation()));
        params.put("created_at", annotation.getCreatedAt());

        SqlHelper.handleSave(getHandle(), "lw_resource_annotation", params).execute();
    }

    class AnnotationMapper implements RowMapper<Annotation> {
        @Override
        public Annotation map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Annotation log = new Annotation();
            log.setAnnotationId(rs.getInt("annotation_id"));
            log.setResourceId(rs.getInt("resource_id"));
            log.setUserId(rs.getInt("user_id"));
            log.setAction(rs.getString("action"));
            log.setSelection(rs.getString("selection"));
            log.setAnnotation(rs.getString("annotation"));
            log.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            return log;
        }
    }
}
