package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.searchhistory.SearchAnnotation;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

/**
 * @author Philipp Kemkes
 *
 * This class wraps a resource. It's necessary because a resource can appear in various search results with different search terms.
 * Which lead to different text snippets.
 */
public class ResourceDecorator implements Serializable {
    private static final long serialVersionUID = -6611930555147350248L;

    private final Resource resource;
    private int rank;
    private String snippet;
    // the rank which the resource has in the current search result
    private String title;
    private String authorUrl;

    // used for search history
    private boolean clicked;
    private boolean saved;
    private List<SearchAnnotation> annotations;

    public ResourceDecorator(Resource resource) {
        this.resource = resource;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * the rank which the resource has in the current search result.
     */
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getShortSnippet() {
        return Jsoup.clean(StringHelper.shortnString(getSnippet(), 80), Whitelist.none());
    }

    public Resource getResource() {
        return resource;
    }

    // Convenience methods which call the underlying resource

    public String getServiceIcon() {
        return resource.getServiceIcon();
    }

    /**
     * The title with highlighted search terms.
     */
    public String getTitle() {
        if (title != null) {
            return title;
        }

        return resource.getTitle();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(final String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public String getMetadataValue(String key) {
        return resource.getMetadataValue(key);
    }

    public Thumbnail getSmallThumbnail() {
        return resource.getSmallThumbnail();
    }

    public Thumbnail getMediumThumbnail() {
        return resource.getMediumThumbnail();
    }

    public Thumbnail getLargestThumbnail() {
        return resource.getLargestThumbnail();
    }

    public String getLocation() {
        return resource.getLocation();
    }

    public ResourceService getSource() {
        return resource.getSource();
    }

    public String getDescription() {
        return resource.getDescription();
    }

    public String getDescriptionHTML() {
        return resource.getDescriptionHTML();
    }

    public String getEmbedded() {
        return resource.getEmbedded();
    }

    public String getUrl() {
        return resource.getUrl();
    }

    public String getDurationInMinutes() {
        return resource.getDurationInMinutes();
    }

    public int getWidth() {
        return resource.getWidth();
    }

    public int getHeight() {
        return resource.getHeight();
    }

    public Group getGroup() {
        return resource.getGroup();
    }

    public int getGroupId() {
        return resource.getGroupId();
    }

    public int getId() {
        return resource.getId();
    }

    public String getType() {
        return resource.getType().toString();
    }

    public boolean isArchived() {
        return resource.isArchived();
    }

    public ArchiveUrl getFirstArchivedObject() {
        return resource.getFirstArchivedObject();
    }

    public ArchiveUrl getLastArchivedObject() {
        return resource.getLastArchivedObject();
    }

    public User getUser() {
        return resource.getUser();
    }

    public String getFileName() {
        return resource.getFileName();
    }

    public double getStarRating() {
        return resource.getStarRating();
    }

    public int getRateNumber() {
        return resource.getRateNumber();
    }

    public int getRatingSum() {
        return resource.getRatingSum();
    }

    //getters for new variables for extended metadata (Chloe)

    public String getLanguage() {
        return resource.getLanguage();
    }

    public Boolean isEditLocked() {
        return resource.isEditLocked();
    }

    public boolean getClicked() {
        return clicked;
    }

    public void setClicked(final boolean clicked) {
        this.clicked = clicked;
    }

    public boolean getSaved() {
        return saved;
    }

    public void setSaved(final boolean saved) {
        this.saved = saved;
    }

    public List<SearchAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(final List<SearchAnnotation> annotations) {
        this.annotations = annotations;
    }

    @Override
    public String toString() {
        return "ResourceDecorator [resource=" + resource + ", rank=" + rank + ", snippet=" + snippet + ", title=" + title + "]";
    }
}
