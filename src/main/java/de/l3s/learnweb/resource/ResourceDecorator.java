package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.archive.ArchiveUrl;
import de.l3s.learnweb.resource.web.WebResource;
import de.l3s.learnweb.user.User;
import de.l3s.util.StringHelper;

/**
 * @author Philipp Kemkes
 *
 * This class wraps a resource. It's necessary because a resource can appear in various search results with different search terms.
 * Which lead to different text snippets.
 */
public class ResourceDecorator implements Serializable {
    @Serial
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
        return Jsoup.clean(StringHelper.shortnString(getSnippet(), 80), Safelist.none());
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

    public String getThumbnailSmallest() {
        return resource.getThumbnailSmallest();
    }

    public String getThumbnailMedium() {
        if (resource.getThumbnailLargest() != null) {
            return resource.getThumbnailMedium();
        }
        if (resource.getThumbnailLarge() != null) {
            return resource.getThumbnailLarge();
        }
        return resource.getThumbnailSmall();
    }

    public String getThumbnailLargest() {
        return resource.getThumbnailLargest();
    }

    public ResourceService getService() {
        return resource.getService();
    }

    public String getDescription() {
        return resource.getDescription();
    }

    public String getEmbeddedCode() {
        return resource.getEmbeddedCode();
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
        if (resource instanceof WebResource web) {
            return web.isArchived();
        }
        return false;
    }

    public ArchiveUrl getFirstArchivedObject() {
        if (resource instanceof WebResource web) {
            return web.getFirstArchivedObject();
        }
        return null;
    }

    public ArchiveUrl getLastArchivedObject() {
        if (resource instanceof WebResource web) {
            return web.getLastArchivedObject();
        }
        return null;
    }

    public User getUser() {
        return resource.getUser();
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

    @Override
    public String toString() {
        return "ResourceDecorator [resource=" + resource + ", rank=" + rank + ", snippet=" + snippet + ", title=" + title + "]";
    }
}
