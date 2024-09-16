package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(TokenDao.TokenMapper.class)
public interface TokenDao extends SqlObject, Serializable {

    @CreateSqlObject
    UserDao getUserDao();

    @SqlQuery("SELECT * FROM lw_user_token WHERE token_id = ?")
    Optional<Token> findById(int tokenId);

    @SqlQuery("SELECT * FROM lw_user_token WHERE type = ? AND user_id = ? LIMIT 1")
    Optional<Token> findByTypeAndUser(Token.TokenType type, int userId);

    /**
     * Retrieves token by type and userId from database or creates a new one if none exists.
     */
    default String findOrCreate(Token.TokenType type, int userId) {
        Optional<Token> retrievedToken = findByTypeAndUser(type, userId);

        if (retrievedToken.isPresent() && retrievedToken.get().isExpired()) {
            delete(retrievedToken.get().getId());
            retrievedToken = Optional.empty();
        }

        if (retrievedToken.isEmpty()) {
            String token = RandomStringUtils.secure().nextAlphanumeric(128);
            insert(userId, type, token, SqlHelper.now().plusYears(1));
            return token;
        }

        return retrievedToken.get().getToken();
    }

    default Optional<User> findUserByToken(int tokenId, String token) {
        Optional<Token> retrievedToken = findById(tokenId);

        if (retrievedToken.isPresent()) {
            if (!retrievedToken.get().isExpired() && retrievedToken.get().getToken().equals(token)) {
                return getUserDao().findById(retrievedToken.get().getUserId());
            } else {
                delete(retrievedToken.get().getId()); // it is expired, or someone trying to hijack it
            }
        }

        return Optional.empty();
    }

    @SqlUpdate("INSERT INTO lw_user_token (user_id, type, token, expires) VALUES(?, ?, ?, ?)")
    @GetGeneratedKeys("token_id")
    int insert(int userId, Token.TokenType type, String token, LocalDateTime expires);

    default int override(int userId, Token.TokenType type, String token, LocalDateTime expires) {
        deleteByTypeAndUser(type, userId);
        return insert(userId, type, token, expires);
    }

    @SqlUpdate("DELETE FROM lw_user_token WHERE token_id = ?")
    void delete(int tokenId);

    @SqlUpdate("DELETE FROM lw_user_token WHERE type = ? AND user_id = ?")
    void deleteByTypeAndUser(Token.TokenType type, int userId);

    class TokenMapper implements RowMapper<Token> {
        @Override
        public Token map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new Token(
                rs.getInt("token_id"),
                rs.getInt("user_id"),
                rs.getString("type"),
                rs.getString("token"),
                SqlHelper.getLocalDateTime(rs.getTimestamp("expires")),
                SqlHelper.getLocalDateTime(rs.getTimestamp("created_at"))
            );
        }
    }
}
