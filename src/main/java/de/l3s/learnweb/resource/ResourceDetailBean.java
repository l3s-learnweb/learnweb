package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
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
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.User;
import de.l3s.util.UrlHelper;

@Named
@ViewScoped
public class ResourceDetailBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 4911923763255682055L;
    private static final Logger log = LogManager.getLogger(ResourceDetailBean.class);

    private static final String HYPOTHESIS_PROXY = "https://via.hypothes.is/";

    public enum ViewAction {
        viewResource,
        editResource
    }

    private String newTag;
    private String newComment;

    // Url params
    private int resourceId = 0;
    private boolean editResource = false;

    private Resource resource;
    private ViewAction viewAction = ViewAction.viewResource;
    private List<LogEntry> logs;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private CommentDao commentDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        resource = resourceDao.findByIdOrElseThrow(resourceId);
        BeanAssert.notDeleted(resource);
        BeanAssert.hasPermission(resource.canViewResource(getUser()));

        log(Action.opening_resource, this.resource.getGroupId(), this.resource.getId());

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
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

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
            PrimeFaces.current().ajax().update(":resourceViewForm");
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
            addErrorMessage(e);
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
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
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
                getLearnweb().getResourcePreviewMaker().processPdf(resource, UrlHelper.getInputStream(archiveUrl));
            } else {
                getLearnweb().getResourcePreviewMaker().processArchivedVersion(resource, archiveUrl);
            }

            resource.save();
            log(Action.resource_thumbnail_update, resource.getGroupId(), resource.getId(), "");
            addGrowl(FacesMessage.SEVERITY_INFO, "Successfully updated the thumbnail");
        } catch (RuntimeException | IOException e) {
            addErrorMessage(e);
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

    public boolean isStarRatedByUser() {
        if (getUser() == null || null == resource) {
            return false;
        }

        return resource.isRatedByUser(getUser().getId());
    }

    public boolean isThumbRatedByUser() {
        if (getUser() == null || null == resource) {
            return false;
        }

        return resource.isThumbRatedByUser(getUser());
    }

    public void handleRate(RateEvent<Integer> rateEvent) {
        if (null == getUser()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        try {
            if (isStarRatedByUser()) {
                addGrowl(FacesMessage.SEVERITY_FATAL, "resource_already_rated");
                return;
            }

            resource.rate(rateEvent.getRating(), getUser());
        } catch (Exception e) {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.rating_resource, resource.getGroupId(), resource.getId());

        addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    private void handleThumbRating(int direction) {
        if (null == getUser()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        try {
            if (isThumbRatedByUser()) {
                addGrowl(FacesMessage.SEVERITY_FATAL, "resource_already_rated");
                return;
            }

            resource.thumbRate(getUser(), direction);
        } catch (Exception e) {
            addGrowl(FacesMessage.SEVERITY_FATAL, "error while rating");
            log.error("error while rating", e);
            return;
        }

        log(Action.thumb_rating_resource, resource.getGroupId(), resource.getId());

        addGrowl(FacesMessage.SEVERITY_INFO, "resource_rated");
    }

    public void onThumbUp() {
        handleThumbRating(1);
    }

    public void onThumbDown() {
        handleThumbRating(-1);
    }

    public String getHypothesisLink() {
        return HYPOTHESIS_PROXY + resource.getUrl();
    }

    public List<LogEntry> getLogs() {
        if (null == logs) {
            logs = getResource().getLogs();
        }
        return logs;
    }
}
