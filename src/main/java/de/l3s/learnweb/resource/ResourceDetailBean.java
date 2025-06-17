package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.PrimeFaces;
import org.primefaces.event.RateEvent;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 4911923763255682055L;
    private static final Logger log = LogManager.getLogger(ResourceDetailBean.class);

    public enum ViewAction {
        viewResource,
        editResource
    }

    private String newTag;
    private String newComment;

    // Url params
    private int resourceId = 0;
    private int tab = 0;
    private boolean aside = true;
    private boolean editResource = false;

    private Resource resource;
    private HashMap<String, Integer> ratingValues;
    private int embeddedTab = 0;
    private ViewAction viewAction = ViewAction.viewResource;

    private transient List<LogEntry> logs;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private CommentDao commentDao;

    public void onLoad() {
        resource = resourceDao.findByIdOrElseThrow(resourceId);
        BeanAssert.notDeleted(resource);

        if (!resource.canViewResource(getUser())) {
            if (isLoggedIn()) {
                throw new ForbiddenHttpException();
            } else {
                throw new UnauthorizedHttpException();
            }
        }

        log(Action.opening_resource, this.resource.getGroupId(), this.resource.getId());

        embeddedTab = resource.getDefaultTab().ordinal();
        if (editResource) {
            editResource();
        }
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getTab() {
        return tab;
    }

    public void setTab(final int tab) {
        this.tab = tab;
    }

    public boolean getAside() {
        return aside;
    }

    public void setAside(final boolean aside) {
        this.aside = aside;
    }

    public boolean isEditResource() {
        return editResource;
    }

    public void setEditResource(final boolean editResource) {
        this.editResource = editResource;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public int getEmbeddedTab() {
        return embeddedTab;
    }

    public void setEmbeddedTab(final int embeddedTab) {
        this.embeddedTab = embeddedTab;
    }

    public ViewAction getViewAction() {
        return viewAction;
    }

    public void setViewAction(ViewAction viewAction) {
        this.viewAction = viewAction;
    }

    public void editResource() {
        releaseResourceIfLocked();
        if (!resource.lockResource(getUser())) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.locked_by_user", resource.getLockUsername());
            log(Action.lock_rejected_edit_resource, resource.getGroupId(), resource.getId());
            return;
        }

        log(Action.opening_resource, resource.getGroupId(), resource.getId());
        viewAction = ViewAction.editResource;
    }

    public void saveEdit() {
        BeanAssert.hasPermission(resource.canEditResource(getUser()));

        resource.save();

        log(Action.edit_resource, resource.getGroupId(), resource.getId(), resource.getTitle());
        addMessage(FacesMessage.SEVERITY_INFO, "changes_saved");

        resource.unlockResource(getUser());
        viewAction = ViewAction.viewResource;
    }

    public void cancelEdit() {
        releaseResourceIfLocked();
        viewAction = ViewAction.viewResource;
    }

    public void releaseResourceIfLocked() {
        if (resource != null && resource.isEditLocked()) {
            resource.unlockResource(getUser());
        }
    }

    public void editActivityListener() {
        if (resource != null && !resource.lockerUpdate(getUser())) {
            releaseResourceIfLocked();
            log(Action.lock_interrupted_returned_resource, resource.getGroupId(), resource.getId());
            addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.edit_interrupted");

            viewAction = ViewAction.viewResource;
            PrimeFaces.current().ajax().update(":resource_view");
        }
    }

    public void onDeleteTag(Tag tag) {
        resource.deleteTag(tag);
        addMessage(FacesMessage.SEVERITY_INFO, "tag_deleted");
    }

    public void addTag() {
        if (null == getUser()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        if (StringUtils.isBlank(newTag)) {
            return;
        }

        //Limit number of spaces in a tag = 3
        if ((StringUtils.countMatches(newTag, " ") > 3) || newTag.contains(",") || newTag.contains("#") || (newTag.length() > 50)) {
            showTagWarningMessage();
            return;
        }

        resource.addTag(newTag, getUser());
        addGrowl(FacesMessage.SEVERITY_INFO, "tag_added");
        log(Action.tagging_resource, resource.getGroupId(), resource.getId(), newTag);
        newTag = ""; // clear tag input field
    }

    /**
     * Recreates the thumbnails of the selected resource.
     */
    public void onUpdateThumbnail() {
        try {
            User user = getUser();
            if (user == null || !user.isAdmin()) {
                return;
            }

            getLearnweb().getResourcePreviewMaker().processResource(resource);

            resource.save();
            releaseResourceIfLocked();
            viewAction = ViewAction.viewResource;
        } catch (IOException e) {
            throw new HttpException("Failed to update thumbnail", e);
        }
    }

    private void showTagWarningMessage() {
        String title = getLocaleMessage("incorrect_tags");
        String text;

        if (newTag.contains("#")) {
            text = getLocaleMessage("tags_hashtag") + newTag.replaceAll("#", " ");
        } else if (newTag.contains(",")) {
            text = getLocaleMessage("tags_specialCharacter");
        } else if ((StringUtils.countMatches(newTag, " ") > 3)) {
            text = getLocaleMessage("tags_spaces");
        } else {
            text = getLocaleMessage("tags_tooLong");
        }

        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, title, text);
        PrimeFaces.current().dialog().showMessageDynamic(message);
    }

    public boolean canEditComment(Comment comment) {
        User user = getUser();
        if (null == user) {
            return false;
        }
        if (user.isAdmin() || user.isModerator()) {
            return true;
        }

        User owner = comment.getUser();
        return user.equals(owner);
    }

    public boolean canDeleteTag(Tag tag) {
        User user = getUser();
        if (null == user) {
            return false;
        }
        if (user.isAdmin() || user.isModerator()) {
            return true;
        }

        User owner = resource.getTags().getElementOwner(tag);
        return user.equals(owner);
    }

    public void onEditComment(Comment comment) {
        commentDao.save(comment);
        addMessage(FacesMessage.SEVERITY_INFO, "changes_saved");
    }

    public void onDeleteComment(Comment comment) {
        resource.deleteComment(comment);
        addMessage(FacesMessage.SEVERITY_INFO, "comment_deleted");
        log(Action.deleting_comment, resource.getGroupId(), comment.getResourceId(), comment.getId());
    }

    public void addComment() {
        Comment comment = resource.addComment(newComment, getUser());
        log(Action.commenting_resource, resource.getGroupId(), resource.getId(), comment.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "comment_added");
        newComment = "";
    }

    public void setResourceThumbnail(String archiveUrl) {
        try {
            //Getting mime type
            FileInspector.FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(archiveUrl);
            String type = info.getMimeType().substring(0, info.getMimeType().indexOf('/'));
            if (type.equals("application")) {
                type = info.getMimeType().substring(info.getMimeType().indexOf('/') + 1);
            }

            if (type.equalsIgnoreCase("pdf")) {
                getLearnweb().getResourcePreviewMaker().processDocument(resource, archiveUrl);
            } else {
                getLearnweb().getResourcePreviewMaker().processWebsite(resource, archiveUrl);
            }

            resource.save();
            log(Action.resource_thumbnail_update, resource.getGroupId(), resource.getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        } catch (RuntimeException | IOException e) {
            throw new HttpException("Failed to set thumbnail", e);
        }
    }

    // ------------------- Simple getters and setters ---------------------------

    public String getNewTag() {
        return newTag;
    }

    public void setNewTag(String newTag) {
        this.newTag = newTag;
    }

    public String getNewComment() {
        return newComment;
    }

    public void setNewComment(String newComment) {
        this.newComment = newComment;
    }

    public HashMap<String, Integer> getRatingValues() {
        if (null == ratingValues) {
            ratingValues = new HashMap<>();
            if (isLoggedIn()) {
                resource.getRatings().forEach((key, value) -> ratingValues.put(key, value.getRate(getUser().getId())));
            }
        }
        return ratingValues;
    }

    public void onThumbUp() {
        handleRate("thumb", 1);
    }

    public void onThumbDown() {
        handleRate("thumb", -1);
    }

    public void handleRate(RateEvent<Object> rateEvent) {
        String ratingType = rateEvent.getComponent().getId();
        int ratingValue = Integer.parseInt((String) rateEvent.getRating());
        handleRate(ratingType, ratingValue);
    }

    public void handleRate(String ratingType, int ratingValue) {
        if (null == getUser()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        try {
            if (resource.isRated(getUser().getId(), ratingType)) {
                addGrowl(FacesMessage.SEVERITY_FATAL, "resource_already_rated");
                return;
            }

            resource.rate(getUser(), ratingType, ratingValue);
        } catch (Exception e) {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.rating_resource, resource.getGroupId(), resource.getId());

        addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    public List<LogEntry> getLogs() {
        if (null == logs && resource != null) {
            logs = resource.getLogs();
        }
        return logs;
    }
}
