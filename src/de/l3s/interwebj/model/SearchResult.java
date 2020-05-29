package de.l3s.interwebj.model;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;

import com.google.gson.annotations.SerializedName;

public class SearchResult implements Serializable {
    private static final long serialVersionUID = -3918531133914589219L;

    @SerializedName("service")
    private String service;
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
    @SerializedName("image")
    private String image; // TODO: remove?
    @SerializedName("date")
    private String date;
    @SerializedName("tags")
    private String tags;
    @SerializedName("rank_at_service")
    private Integer rankAtService; // TODO: remove?
    @SerializedName("total_results_at_service")
    private Integer totalResultsAtService; // TODO: remove?
    @SerializedName("views")
    private Integer numberOfViews; // TODO: remove?
    @SerializedName("number_of_comments")
    private Integer numberOfComments; // TODO: remove?
    /**
     * TODO: add a type (small/medium/large/original) field or
     * remove and replace embedded (change their type to SearchThumbnail, add `embedded` field to SearchThumbnail)
     */
    @SerializedName("thumbnail")
    private List<SearchThumbnail> thumbnails;
    /**
     * html code, only image or text, max width and max height 100px.
     * TODO:
     * Used for small images preview in results from different services. Propose to define as image with height between 100 and 150 px.
     * In case of missing fallback to size2 or size3.
     */
    @SerializedName("embedded_size1")
    private String embeddedSize1;
    /**
     * html code, only image or text, max width and max height 240px.
     * TODO:
     * Used for results grid. Propose to define as image with height between 200 and 300 px.
     * In case of missing fallback to size3 and size1.
     */
    @SerializedName("embedded_size2")
    private String embeddedSize2;
    /**
     * html code, could be flash, max width and max height 500px.
     * TODO:
     * Used for resource details view in lightbox. Propose to define as image with height between 600 and 1800 px.
     * In case of missing fallback to original or size2.
     */
    @SerializedName("embedded_size3")
    private String embeddedSize3;
    /**
     * html code, could be flash, max width and max height 100%.
     * TODO:
     * Original image/view in max quality. Used only as link.
     */
    @SerializedName("embedded_size4")
    private String embeddedSize4;
    /**
     * Url to the best (high resolution) available preview image.
     * TODO:
     * Remove?
     */
    @SerializedName("max_image_url")
    private String imageUrl; // TODO: remove?
    @SerializedName("duration")
    private Integer duration;
    @SerializedName("snippet")
    private String snippet;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getIdAtService() {
        return idAtService;
    }

    public void setIdAtService(String idAtService) {
        this.idAtService = idAtService;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<SearchThumbnail> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<SearchThumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getRankAtService() {
        return rankAtService;
    }

    public void setRankAtService(Integer rankAtService) {
        this.rankAtService = rankAtService;
    }

    public Integer getTotalResultsAtService() {
        return totalResultsAtService;
    }

    public void setTotalResultsAtService(Integer totalResultsAtService) {
        this.totalResultsAtService = totalResultsAtService;
    }

    public Integer getNumberOfViews() {
        return numberOfViews;
    }

    public void setNumberOfViews(Integer numberOfViews) {
        this.numberOfViews = numberOfViews;
    }

    public Integer getNumberOfComments() {
        return numberOfComments;
    }

    public void setNumberOfComments(Integer numberOfComments) {
        this.numberOfComments = numberOfComments;
    }

    public String getEmbeddedSize1() {
        return embeddedSize1;
    }

    public void setEmbeddedSize1(String embeddedSize1) {
        this.embeddedSize1 = embeddedSize1;
    }

    public String getEmbeddedSize2() {
        return embeddedSize2;
    }

    public void setEmbeddedSize2(String embeddedSize2) {
        this.embeddedSize2 = embeddedSize2;
    }

    public String getEmbeddedSize3() {
        return embeddedSize3;
    }

    public void setEmbeddedSize3(String embeddedSize3) {
        this.embeddedSize3 = embeddedSize3;
    }

    public String getEmbeddedSize4() {
        return embeddedSize4;
    }

    public void setEmbeddedSize4(String embeddedSize4) {
        this.embeddedSize4 = embeddedSize4;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SearchResult.class.getSimpleName() + "[", "]")
            .add("service='" + service + "'")
            .add("idAtService='" + idAtService + "'")
            .add("type='" + type + "'")
            .add("title='" + title + "'")
            .add("description='" + description + "'")
            .add("url='" + url + "'")
            .add("image='" + image + "'")
            .add("thumbnails=" + thumbnails)
            .add("date='" + date + "'")
            .add("tags='" + tags + "'")
            .add("rankAtService=" + rankAtService)
            .add("totalResultsAtService=" + totalResultsAtService)
            .add("numberOfViews=" + numberOfViews)
            .add("numberOfComments=" + numberOfComments)
            .add("embeddedSize1='" + embeddedSize1 + "'")
            .add("embeddedSize2='" + embeddedSize2 + "'")
            .add("embeddedSize3='" + embeddedSize3 + "'")
            .add("embeddedSize4='" + embeddedSize4 + "'")
            .add("imageUrl='" + imageUrl + "'")
            .add("duration=" + duration)
            .add("snippet='" + snippet + "'")
            .toString();
    }
}
