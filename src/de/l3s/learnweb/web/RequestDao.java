package de.l3s.learnweb.web;

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

import de.l3s.util.RsHelper;

public interface RequestDao extends SqlObject {
    @SqlQuery("SELECT * FROM lw_requests WHERE IP = ?")
    @RegisterRowMapper(RequestMapper.class)
    List<Request> getRequests();

    @SqlQuery("SELECT * FROM lw_requests WHERE ip = ?")
    @RegisterRowMapper(RequestMapper.class)
    List<Request> getRequestsByIp(String ip);

    @SqlQuery("SELECT * FROM lw_requests WHERE time >= ?")
    @RegisterRowMapper(RequestMapper.class)
    List<Request> getRequestsAfterDate(LocalDateTime date);

    @SuppressWarnings("SqlWithoutWhere")
    @SqlUpdate("DELETE FROM lw_requests")
    void deleteAllRequests();

    default void save(Request request) {
        getHandle().createUpdate("INSERT INTO lw_requests (IP, requests, logins, usernames, time) VALUES(?, ?, ?, ?, ?);")
            .bind(0, request.getIp())
            .bind(1, request.getRequests())
            .bind(2, request.getLoginCount())
            .bind(3, request.getUsernames())
            .bind(4, request.getTime())
            .execute();
    }

    default void saveAll(Iterable<Request> requests) {
        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_requests (IP, requests, logins, usernames, time) VALUES(?, ?, ?, ?, ?)");
        for (Request request : requests) {
            batch.bind(0, request.getIp())
                .bind(1, request.getRequests())
                .bind(2, request.getLoginCount())
                .bind(3, request.getUsernames())
                .bind(4, request.getTime())
                .add();
        }
        batch.execute();
    }

    class RequestMapper implements RowMapper<Request> {
        @Override
        public Request map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Request request = new Request(rs.getString("ip"), null);
            request.setRequests(rs.getInt("requests"));
            request.setLoginCount(rs.getInt("logins"));
            request.setUsernames(rs.getString("usernames"));
            request.setTime(RsHelper.getLocalDateTime(rs.getTimestamp("time")));
            return request;
        }
    }
}
