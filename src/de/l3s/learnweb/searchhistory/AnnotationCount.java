package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AnnotationCount implements Serializable {

    @Serial
    private static final long serialVersionUID = 5389844250794875603L;
    private LocalDateTime createdAt;
    private double confidence;
    private String surfaceForm;
    private int repetition;
    private String uri;
    private String type;
    private int id;
    private int uri_id;
    private String sessionId;
    private String users;

    public String getUsers() {
        return users;
    }

    public void setUsers(final String users) {
        this.users = users;
    }

    public void addUser(String user) {
        this.users += "," + user;
    }

    public int getUri_id() {
        return uri_id;
    }

    public void setUri_id(final int uri_id) {
        this.uri_id = uri_id;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public AnnotationCount(final int id, final double confidence, final String surfaceForm, final String uri, final String type
        , final String users, final String sessionId) {
        this.id = id;
        this.confidence = confidence;
        this.surfaceForm = surfaceForm;
        this.uri = uri;
        this.type = type;
        this.users = users;
        this.createdAt = LocalDateTime.now();
        this.repetition = 1;
        this.sessionId = sessionId;
    }

    public AnnotationCount() {

    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(final double confidence) {
        this.confidence = confidence;
    }

    public String getSurfaceForm() {
        return surfaceForm;
    }

    public void setSurfaceForm(final String surfaceForm) {
        this.surfaceForm = surfaceForm;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public int getRepetition() {
        return repetition;
    }

    public void setRepetition(final int repetition) {
        this.repetition = repetition;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public void addSessionId(final String sessionId) {
        this.sessionId += "," + sessionId;
    }
}
