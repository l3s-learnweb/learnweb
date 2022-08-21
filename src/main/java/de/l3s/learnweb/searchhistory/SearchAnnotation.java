package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;

public class SearchAnnotation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1311485147202161998L;

    private int userId;
    private String text;
    private String quote;
    private String targetUrl;

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(final String quote) {
        this.quote = quote;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(final String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
