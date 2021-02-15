package de.l3s.learnweb.dashboard.glossary;

import java.io.Serializable;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.user.User;

public class GlossaryDescriptionSummary implements Serializable {
    private static final long serialVersionUID = -3538944153978434822L;

    private int userId;
    private String description;
    private boolean descriptionPasted;
    private int length;
    private int entryId;
    private int resourceId;

    private transient User user;

    public User getUser() {
        if (null == user && userId > 0) {
            user = Learnweb.dao().getUserDao().findById(userId);
        }
        return user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        if (description != null && !description.isEmpty()) {
            this.length = description.split(" ").length;
        }
    }

    public boolean isDescriptionPasted() {
        return descriptionPasted;
    }

    public void setDescriptionPasted(boolean descriptionPasted) {
        this.descriptionPasted = descriptionPasted;
    }

    /**
     * @return Word count of the description (calculation is to simplified)
     */
    public int getLength() {
        return length;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

}
