package de.l3s.learnweb.searchhistory;

import java.sql.Date;

public class InputStreamRdf {

    private int id;
    private String content;
    private String type;
    private Date dateCreated;
    private int userId;

    public InputStreamRdf(final int id, final int userId, final String content, final String type, final Date dateCreated) {
        this.userId = userId;
        this.id = id;
        this.content = content;
        this.type = type;
        this.dateCreated = dateCreated;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(final Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
