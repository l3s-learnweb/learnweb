package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SearchQuery implements Serializable {
    private static final long serialVersionUID = 4391998336381044255L;

    private final int searchId;
    private final String query;
    private final String mode;
    private final LocalDateTime timestamp;
    private final String service;

    public SearchQuery(int searchId, String query, String mode, LocalDateTime timestamp, String service) {
        this.searchId = searchId;
        this.query = query;
        this.mode = mode;
        this.timestamp = timestamp;
        this.service = service;
    }

    public int getSearchId() {
        return this.searchId;
    }

    public String getQuery() {
        return this.query;
    }

    public String getMode() {
        return mode;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public String getService() {
        return this.service;
    }
}
