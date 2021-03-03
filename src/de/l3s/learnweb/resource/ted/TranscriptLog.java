package de.l3s.learnweb.resource.ted;

import java.io.Serializable;
import java.time.Instant;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

public class TranscriptLog implements Serializable {
    private static final long serialVersionUID = 6321296603254649454L;

    private int userId;
    private int resourceId;
    private String wordsSelected;
    private String userAnnotation;
    private String action;
    private Instant timestamp;

    // cached values
    private transient User user;
    private transient Resource resource;

    public TranscriptLog() {

    }

    public TranscriptLog(int userId, int resourceId, String wordsSelected, String userAnnotation, String action, Instant timestamp) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.wordsSelected = wordsSelected;
        this.userAnnotation = userAnnotation;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getWordsSelected() {
        return wordsSelected;
    }

    public void setWordsSelected(String wordsSelected) {
        this.wordsSelected = wordsSelected;
    }

    public String getUserAnnotation() {
        return userAnnotation;
    }

    public void setUserAnnotation(String userAnnotation) {
        this.userAnnotation = userAnnotation;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    // ------------ convenience functions -----------------

    public User getUser() {
        if (null == user) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(getUserId());
        }
        return user;
    }

    public Resource getResource() {
        if (null == resource) {
            resource = Learnweb.dao().getResourceDao().findByIdOrElseThrow(resourceId);
        }
        return resource;
    }
}
