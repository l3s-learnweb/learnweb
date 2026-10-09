package de.l3s.learnweb.resource;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;

public class Annotation implements Serializable {
    @Serial
    private static final long serialVersionUID = 6321296603254649454L;

    /**
     * Action values stored by annotator.js mapped to their message keys.
     */
    public static final SequencedMap<String, String> ACTIONS;

    static {
        SequencedMap<String, String> actions = new LinkedHashMap<>();
        actions.put("selection", "transcript_log.selection");
        actions.put("deselection", "transcript_log.deselection");
        actions.put("add annotation", "add_annotation");
        actions.put("edit annotation", "edit_annotation");
        actions.put("delete annotation", "delete_annotation");
        actions.put("display definition", "transcript_log.display_definition");
        actions.put("save transcript", "transcript_log.save_transcript");
        actions.put("submit transcript", "transcript_log.submit_transcript");
        ACTIONS = Collections.unmodifiableSequencedMap(actions);
    }

    private int annotationId;
    private int resourceId;
    private int userId;
    private String action;
    private String selection;
    private String annotation;
    private Instant createdAt;

    // cached values
    private transient User user;
    private transient Resource resource;

    public Annotation() {
    }

    public int getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(final int annotationId) {
        this.annotationId = annotationId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(final String selection) {
        this.selection = selection;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(final String annotation) {
        this.annotation = annotation;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the message key to display the stored action, or the stored value itself if it is unknown
     */
    public String getActionMsgKey() {
        return ACTIONS.getOrDefault(action, action);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        if (null == user) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(userId);
        }
        return user;
    }

    public Resource getResource() {
        if (null == resource) {
            resource = Learnweb.dao().getResourceDao().findByIdOrElseThrow(resourceId);
        }
        return resource;
    }
}
