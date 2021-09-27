package de.l3s.learnweb.web;

import java.io.Serial;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(BounceDao.BounceMapper.class)
public interface BounceDao extends SqlObject, Serializable {

    @SqlQuery("SELECT MAX(received) FROM lw_bounces")
    Optional<Instant> findLastBounceDate();

    @SqlUpdate("INSERT INTO lw_bounces (email, received, code, description) VALUES (?, ?, ?, ?) ON DUPLICATE KEY "
        + "UPDATE received = values(received), code = values(code), description = values(description)")
    void save(String email, Instant date, String code, String description);

    @SqlQuery("SELECT * FROM lw_bounces")
    List<Bounce> findAll();

    @SqlQuery("SELECT * FROM lw_bounces WHERE email = ?")
    Optional<Bounce> findByEmail(String email);

    class BounceMapper implements RowMapper<Bounce> {
        @Override
        public Bounce map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Bounce post = new Bounce(
                rs.getInt("bounce_id"),
                rs.getString("email"),
                rs.getString("code"),
                rs.getString("description"),
                SqlHelper.getInstant(rs.getTimestamp("received"))
            );
            return post;
        }
    }

    record Bounce(int id, String email, String errorCode, String description, Instant received) implements Serializable {
        @Serial
        private static final long serialVersionUID = -6899053136547019703L;
    }
}
