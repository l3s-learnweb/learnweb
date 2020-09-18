
package de.l3s.learnweb.hserver.entities;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.json.bind.annotation.JsonbTypeAdapter;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.hserver.adapters.AnnotationAdapter;
import de.l3s.learnweb.user.User;

@JsonbTypeAdapter(AnnotationAdapter.class)
public class Annotation implements Serializable {
    private static final long serialVersionUID = 8441658438245798148L;

    private Integer id;
    private Integer userId;
    private Integer groupId;
    private Integer documentId;
    private String text;
    private String textRendered;
    private String tags; // List<String> tags
    private Boolean shared;
    private String targetUri;
    private String targetUriNormalized;
    private String targetSelectors; // List<Target> target
    private String references; // List<String> target
    private String extra;
    private Boolean deleted;
    private LocalDateTime created;
    private LocalDateTime updated;

    private transient User user;
    private transient Group group;
    private transient Document document;
    private transient Document unsavedDocument;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public User getUser() throws SQLException {
        if (user == null && userId != null && userId > 0) {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(final Integer groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() throws SQLException {
        if (group == null && groupId != null && groupId > 0) {
            group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
        }
        return group;
    }

    public Integer getDocumentId() {
        return documentId;
    }

    public void setDocumentId(final Integer documentId) {
        this.documentId = documentId;
    }

    public Document getDocument() {
        if (document == null && documentId != null && documentId > 0) {
            document = Learnweb.getInstance().getDocumentManager().get(documentId);
        }
        return document;
    }

    public void setDocument(final Document document) {
        if (document.getId() != null) {
            this.documentId = document.getId();
            this.document = document;
        }
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getTextRendered() {
        return textRendered;
    }

    public void setTextRendered(final String textRendered) {
        this.textRendered = textRendered;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(final String tags) {
        this.tags = tags;
    }

    public Boolean getShared() {
        return shared;
    }

    public void setShared(final Boolean shared) {
        this.shared = shared;
    }

    public String getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(final String targetUri) {
        this.targetUri = targetUri;
    }

    public String getTargetUriNormalized() {
        return targetUriNormalized;
    }

    public void setTargetUriNormalized(final String targetUriNormalized) {
        this.targetUriNormalized = targetUriNormalized;
    }

    public String getTargetSelectors() {
        return targetSelectors;
    }

    public void setTargetSelectors(final String targetSelectors) {
        this.targetSelectors = targetSelectors;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(final String references) {
        this.references = references;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(final String extra) {
        this.extra = extra;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(final LocalDateTime updated) {
        this.updated = updated;
    }

    public Document getUnsavedDocument() {
        return unsavedDocument;
    }

    public void setUnsavedDocument(final Document unsavedDocument) {
        this.unsavedDocument = unsavedDocument;
    }
}
