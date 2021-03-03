package de.l3s.learnweb.resource.ted;

import java.io.Serializable;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

public class TranscriptSummary implements Serializable {
    private static final long serialVersionUID = -4882492393068437410L;

    private final int userId;
    private final int resourceId;
    private final TedManager.SummaryType summaryType;
    private final String summaryText;

    // cached values
    private transient User user;
    private transient Resource resource;

    public TranscriptSummary(int userId, int resourceId, String summaryType, String summaryText) {
        this.userId = userId;
        this.resourceId = resourceId;
        this.summaryType = TedManager.SummaryType.valueOf(summaryType);
        this.summaryText = summaryText;
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

    public TedManager.SummaryType getSummaryType() {
        return summaryType;
    }

    public String getSummaryText() {
        return summaryText;
    }
}
