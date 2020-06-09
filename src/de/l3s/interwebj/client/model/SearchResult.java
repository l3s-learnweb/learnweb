package de.l3s.interwebj.client.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.gson.annotations.SerializedName;

public class SearchResult implements Serializable {
    private static final long serialVersionUID = -3918531133914589219L;

    @SerializedName("service")
    private String service;
    @SerializedName("rank_at_service")
    private Integer rankAtService;
    @SerializedName("id_at_service")
    private String idAtService;

    @SerializedName("type")
    private String type;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("url")
    private String url;
    @SerializedName("date")
    private String date;
    @SerializedName("snippet")
    private String snippet;
    @SerializedName("duration")
    private Long duration;
    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("number_of_views")
    private Long numberOfViews;
    @SerializedName("number_of_comments")
    private Long numberOfComments;

    @SerializedName("embedded_code")
    private String embeddedCode;

    /**
     * Usually an image with HEIGHT between 100 and 180 px.
     */
    @SerializedName("thumbnail_small")
    private SearchThumbnail thumbnailSmall;
    /**
     * Usually an image with HEIGHT between 200 and 440 px.
     */
    @SerializedName("thumbnail_medium")
    private SearchThumbnail thumbnailMedium;
    /**
     * Usually an image with HEIGHT between 600 and 920 px.
     */
    @SerializedName("thumbnail_large")
    private SearchThumbnail thumbnailLarge;
    /**
     * Image in max available quality, if bigger than large.
     */
    @SerializedName("thumbnail_original")
    private SearchThumbnail thumbnailOriginal;

    public String getService() {
        return service;
    }

    public void setService(final String service) {
        this.service = service;
    }

    public Integer getRankAtService() {
        return rankAtService;
    }

    public void setRankAtService(final Integer rankAtService) {
        this.rankAtService = rankAtService;
    }

    public String getIdAtService() {
        return idAtService;
    }

    public void setIdAtService(final String idAtService) {
        this.idAtService = idAtService;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(final String snippet) {
        this.snippet = snippet;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(final Long duration) {
        this.duration = duration;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    public Long getNumberOfViews() {
        return numberOfViews;
    }

    public void setNumberOfViews(final Long numberOfViews) {
        this.numberOfViews = numberOfViews;
    }

    public Long getNumberOfComments() {
        return numberOfComments;
    }

    public void setNumberOfComments(final Long numberOfComments) {
        this.numberOfComments = numberOfComments;
    }

    public String getEmbeddedCode() {
        return embeddedCode;
    }

    public void setEmbeddedCode(final String embeddedCode) {
        this.embeddedCode = embeddedCode;
    }

    public SearchThumbnail getThumbnailSmall() {
        return thumbnailSmall;
    }

    public void setThumbnailSmall(final SearchThumbnail thumbnailSmall) {
        this.thumbnailSmall = thumbnailSmall;
    }

    public SearchThumbnail getThumbnailMedium() {
        return thumbnailMedium;
    }

    public void setThumbnailMedium(final SearchThumbnail thumbnailMedium) {
        this.thumbnailMedium = thumbnailMedium;
    }

    public SearchThumbnail getThumbnailLarge() {
        return thumbnailLarge;
    }

    public void setThumbnailLarge(final SearchThumbnail thumbnailLarge) {
        this.thumbnailLarge = thumbnailLarge;
    }

    public SearchThumbnail getThumbnailOriginal() {
        return thumbnailOriginal;
    }

    public void setThumbnailOriginal(final SearchThumbnail thumbnailOriginal) {
        this.thumbnailOriginal = thumbnailOriginal;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("service", service)
            .append("rankAtService", rankAtService)
            .append("idAtService", idAtService)
            .append("type", type)
            .append("title", title)
            .append("description", description)
            .append("url", url)
            .append("date", date)
            .append("snippet", snippet)
            .append("duration", duration)
            .append("tags", tags)
            .append("numberOfViews", numberOfViews)
            .append("numberOfComments", numberOfComments)
            .append("embeddedCode", embeddedCode)
            .append("thumbnailSmall", thumbnailSmall)
            .append("thumbnailMedium", thumbnailMedium)
            .append("thumbnailLarge", thumbnailLarge)
            .append("thumbnailOriginal", thumbnailOriginal)
            .toString();
    }
}
