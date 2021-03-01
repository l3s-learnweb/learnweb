package de.l3s.learnweb.web;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.RsHelper;

@RegisterRowMapper(BanDao.BanMapper.class)
public interface BanDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_bans WHERE addr = ?")
    Optional<Ban> findByAddr(String addr);

    @SqlQuery("SELECT * FROM lw_bans")
    List<Ban> findAll();

    @SqlUpdate("DELETE FROM lw_bans WHERE addr = ?")
    void delete(String addr);

    @SqlUpdate("DELETE FROM lw_bans WHERE expires <= CURDATE() - INTERVAL 7 DAY")
    void deleteOutdated();

    default void save(Ban ban) {
        getHandle().createUpdate("INSERT INTO lw_bans (addr, expires, attempts, reason, created_at) VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE expires = VALUES(expires)")
            .bind(0, ban.getAddr())
            .bind(1, ban.getExpires())
            .bind(2, ban.getAttempts())
            .bind(3, ban.getReason())
            .bind(4, ban.getCreatedAt())
            .execute();
    }

    class BanMapper implements RowMapper<Ban> {
        @Override
        public Ban map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Ban ban = new Ban(rs.getString("addr"));
            ban.setExpires(RsHelper.getLocalDateTime(rs.getTimestamp("expires")));
            ban.setAttempts(rs.getInt("attempts"));
            ban.setReason(rs.getString("reason"));
            ban.setCreatedAt(RsHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return ban;
        }
    }
}
