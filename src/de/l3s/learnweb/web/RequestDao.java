package de.l3s.learnweb.web;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(RequestDao.RequestMapper.class)
public interface RequestDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_requests WHERE addr = ?")
    List<Request> findByIp(String ip);

    @SqlQuery("SELECT * FROM lw_requests WHERE created_at >= ?")
    List<Request> findAfterDate(LocalDateTime date);

    @SuppressWarnings("SqlWithoutWhere")
    @SqlUpdate("DELETE FROM lw_requests")
    void deleteAll();

    default void save(Request request) {
        getHandle().createUpdate("INSERT INTO lw_requests (addr, requests, logins, usernames, created_at) VALUES(?, ?, ?, ?, ?)")
            .bind(0, request.getAddr())
            .bind(1, request.getRequests())
            .bind(2, request.getLoginCount())
            .bind(3, request.getUsernames())
            .bind(4, request.getCreatedAt())
            .execute();
    }

    default void save(Iterable<Request> requests) {
        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_requests (addr, requests, logins, usernames, created_at) VALUES(?, ?, ?, ?, ?)");
        for (Request request : requests) {
            batch.bind(0, request.getAddr())
                .bind(1, request.getRequests())
                .bind(2, request.getLoginCount())
                .bind(3, request.getUsernames())
                .bind(4, request.getCreatedAt())
                .add();
        }
        batch.execute();
    }

    class RequestMapper implements RowMapper<Request> {
        @Override
        public Request map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Request request = new Request(rs.getString("addr"), null);
            request.setRequests(rs.getInt("requests"));
            request.setLoginCount(rs.getInt("logins"));
            request.setUsernames(rs.getString("usernames"));
            request.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return request;
        }
    }
}
