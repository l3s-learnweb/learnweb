package de.l3s.learnweb.resource.ted;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class TedVideo implements Serializable {
    @Serial
    private static final long serialVersionUID = 7530341865385703099L;

    private int tedId;
    private int resourceId;
    private String slug;
    private String title;
    private String description;
    private int viewedCount;
    private LocalDateTime publishedAt;
    private String photoUrl;
    private int photoWidth;
    private int photoHeight;
    private String tags;
    private int duration;

    public int getTedId() {
        return tedId;
    }

    public void setTedId(final int tedId) {
        this.tedId = tedId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(final int resourceId) {
        this.resourceId = resourceId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(final String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public int getViewedCount() {
        return viewedCount;
    }

    public void setViewedCount(final int viewedCount) {
        this.viewedCount = viewedCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(final LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(final String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getPhotoWidth() {
        return photoWidth;
    }

    public void setPhotoWidth(final int photoWidth) {
        this.photoWidth = photoWidth;
    }

    public int getPhotoHeight() {
        return photoHeight;
    }

    public void setPhotoHeight(final int photoHeight) {
        this.photoHeight = photoHeight;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(final String tags) {
        this.tags = tags;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(final int duration) {
        this.duration = duration;
    }
}
