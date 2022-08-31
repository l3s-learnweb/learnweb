package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class AnnotationCount implements Serializable {

    @Serial
    private static final long serialVersionUID = 5389844250794875603L;
    private int frequency = 1;
    private LocalDateTime createdAt;
    private double similarityScore;
    private String surfaceForm;
    private String uri;
    private String type;
    private int id;

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

    public AnnotationCount(final int id, final double similarityScore, final String surfaceForm, final String uri, final String type) {
        this.id = id;
        this.similarityScore = similarityScore;
        this.surfaceForm = surfaceForm;
        this.uri = uri;
        this.type = type;
    }

    public AnnotationCount() {

    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(final double similarityScore) {
        this.similarityScore = similarityScore;
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

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(final int frequency) {
        this.frequency = frequency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
