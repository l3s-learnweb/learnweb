package de.l3s.learnweb.searchhistory;

import java.sql.Date;

public class InputStreamRdf {

    private int id;
    private String content;
    private String type;
    private int objectId;
    private Date dateCreated;
    private int userId;

    public InputStreamRdf(final int id, final int userId, final String type, final int objectId, final String content, final Date dateCreated) {
        this.userId = userId;
        this.id = id;
        this.objectId = objectId;
        this.type = type;
        this.content = content;
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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(final int objectId) {
        this.objectId = objectId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(final Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
