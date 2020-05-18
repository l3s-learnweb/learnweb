package de.l3s.learnweb.resource.ted;

import java.io.Serializable;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

public class TranscriptSummary implements Serializable {
    private static final long serialVersionUID = -4882492393068437410L;

    private final int userId;
    private final int resourceId;
    private String summaryType;
    private String summaryText;

    // cached values
    private transient User user;
    private transient Resource resource;

    public TranscriptSummary(int userId, int resourceId, String summaryType, String summaryText) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.summaryType = summaryType;
        this.summaryText = summaryText;
    }

    public User getUser() throws SQLException {
        if (null == user) {
            user = Learnweb.getInstance().getUserManager().getUser(userId);
        }
        return user;
    }

    public Resource getResource() throws SQLException {
        if (null == resource) {
            resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
        }
        return resource;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }
}
