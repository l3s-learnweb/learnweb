package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AnnotationCount implements Serializable {

    @Serial
    private static final long serialVersionUID = 5389844250794875603L;
    private int uriId;
    private LocalDateTime createdAt;
    private double confidence;
    private String surfaceForm;
    private String uri;
    private String type;
    private String sessionId;
    private int userId;
    //Specifically for web results
    private String inputStreams;

    public int getUriId() {
        return uriId;
    }

    public void setUriId(final int uriId) {
        this.uriId = uriId;
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


    public AnnotationCount(final String id, final double confidence, final String surfaceForm, final String uri, final String type,
        final int userId, final String sessionId) {
        this.confidence = confidence;
        this.surfaceForm = surfaceForm;
        this.uri = uri;
        this.type = type;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
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

    public String getInputStreams() {
        return inputStreams;
    }

    public void setInputStreams(final String inputStreams) {
        this.inputStreams = inputStreams;
    }
}
