package de.l3s.learnweb.user;

import java.time.LocalDateTime;

import de.l3s.util.HasId;

public class Token implements HasId {

    public enum TokenType {
        GRANT, // Used by external services, like Learnweb Annotations
        AUTH, // Used by "Remember me" feature
        EMAIL_CONFIRMATION,
        PASSWORD_RESET,
    }

    private final int id;
    private final int userId;
    private final TokenType type;
    private final String token;
    private final LocalDateTime expires;
    private final LocalDateTime createdAt;

    Token(final int id, final int userId, final String type, final String token, final LocalDateTime expires, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = TokenType.valueOf(type);
        this.token = token;
        this.expires = expires;
        this.createdAt = createdAt;
    }

    @Override
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public TokenType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpires() {
        return expires;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expires);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
