package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.util.LinkedList;
import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;

public class SearchSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 6139247221701183553L;

    private final int userId;
    private final String sessionId;
    private LinkedList<SearchQuery> queries;

    private transient User user;


    public SearchSession(String sessionId, int userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getUser() {
        if (user == null) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public void setQueries(final List<SearchQuery> queries) {
        this.queries = new LinkedList<>(queries);
    }

    public List<SearchQuery> getQueries() {
        return queries;
    }

    public LocalDateTime getStartTimestamp() {
        return queries.getFirst().timestamp();
    }

    public LocalDateTime getEndTimestamp() {
        return queries.getLast().timestamp();
    }
}
