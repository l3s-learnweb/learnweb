package de.l3s.learnweb.web;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.RsHelper;

@RegisterRowMapper(BanDao.BanMapper.class)
public interface BanDao extends SqlObject, Serializable {
    @SqlQuery("SELECT * FROM lw_bans")
    List<Ban> getBans();

    @SqlUpdate("DELETE FROM lw_bans WHERE name = ?")
    void deleteBanByName(String name);

    @SqlUpdate("DELETE FROM lw_bans WHERE bandate <= CURDATE() - INTERVAL 7 DAY")
    void deleteOutdatedBans();

    default void save(Ban ban) {
        getHandle().createUpdate("INSERT INTO lw_bans (type, name, bandate, bannedon, attempts, reason) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE bandate = VALUES(bandate)")
            .bind(0, ban.getType())
            .bind(1, ban.getName())
            .bind(2, ban.getBannedUntil())
            .bind(3, ban.getBannedOn())
            .bind(4, ban.getAttempts())
            .bind(5, ban.getReason())
            .execute();
    }

    class BanMapper implements RowMapper<Ban> {
        @Override
        public Ban map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Ban ban = new Ban(rs.getString("name"));
            ban.setType(rs.getString("type"));
            ban.setBannedUntil(RsHelper.getLocalDateTime(rs.getTimestamp("bandate")));
            ban.setBannedOn(RsHelper.getLocalDateTime(rs.getTimestamp("bannedon")));
            ban.setAttempts(rs.getInt("attempts"));
            ban.setReason(rs.getString("reason"));
            return ban;
        }
    }
}
