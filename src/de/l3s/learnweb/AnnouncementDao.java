package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(AnnouncementDao.AnnouncementMapper.class)
public interface AnnouncementDao extends SqlObject, Serializable {
    @SqlQuery("SELECT * FROM lw_news WHERE news_id = ?")
    Optional<Announcement> findById(int newsId);

    @SqlQuery("SELECT * FROM lw_news ORDER BY created_at DESC")
    List<Announcement> findAll();

    @SqlQuery("SELECT * FROM lw_news WHERE hidden = false ORDER BY created_at DESC LIMIT ?")
    List<Announcement> findLastCreated(int limit);

    @SqlUpdate("DELETE FROM lw_news WHERE news_id = ?")
    void delete(int newsId);

    default void save(Announcement announcement) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("news_id", announcement.getId() < 1 ? null : announcement.getId());
        params.put("title", announcement.getTitle());
        params.put("message", announcement.getText());
        params.put("user_id", announcement.getUserId());
        params.put("hidden", announcement.isHidden());

        SqlHelper.handleSave(getHandle(), "lw_news", params).executeAndReturnGeneratedKeys("news_id", "created_at")
            .map(rowView -> {
                announcement.setId(rowView.getColumn("news_id", Integer.class));
                announcement.setDate(rowView.getColumn("created_at", LocalDateTime.class));
                return announcement;
            }).findOne();
    }

    class AnnouncementMapper implements RowMapper<Announcement> {
        @Override
        public Announcement map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Announcement announcement = new Announcement();
            announcement.setId(rs.getInt("news_id"));
            announcement.setTitle(rs.getString("title"));
            announcement.setText(rs.getString("message"));
            announcement.setUserId(rs.getInt("user_id"));
            announcement.setHidden(rs.getBoolean("hidden"));
            announcement.setDate(RsHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return announcement;
        }
    }
}
