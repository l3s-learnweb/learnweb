package de.l3s.learnweb.resource.ted;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

public class TranscriptLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 6321296603254649454L;

    /**
     * Action values stored by ted-transcript.js mapped to their message keys.
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

    private int userId;
    private int resourceId;
    private String selection;
    private String annotation;
    private String action;
    private Instant timestamp;

    // cached values
    private transient User user;
    private transient Resource resource;

    public TranscriptLog() {

    }

    public TranscriptLog(int userId, int resourceId, String selection, String annotation, String action, Instant timestamp) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.selection = selection;
        this.annotation = annotation;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    // ------------ convenience functions -----------------

    public User getUser() {
        if (null == user) {
            user = Learnweb.dao().getUserDao().findByIdOrElseThrow(getUserId());
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
