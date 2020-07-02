package de.l3s.learnweb.resource.submission;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;

/**
 * This class bundles the submitted resource of one particular user for one submission.
 *
 * @author Philipp Kemkes
 */
public class SubmittedResources implements Serializable {
    private static final long serialVersionUID = -7336342838037456587L;
    private static final Logger log = LogManager.getLogger(SubmittedResources.class);

    private int userId;
    private final List<Resource> resources = new ArrayList<>(); // the submitted resources
    private final int surveyResourceId; // the survey that was used to grade this submission
    private boolean submitStatus; // keeps track of the submit status, so as to lock/unlock a submission

    private transient User user;

    public SubmittedResources(User user, int surveyResourceId, boolean submitStatus) {
        setUser(user);
        this.surveyResourceId = surveyResourceId;
        this.submitStatus = submitStatus;
    }

    public User getUser() {
        if (null == user) {
            try {
                user = Learnweb.getInstance().getUserManager().getUser(userId);
            } catch (SQLException e) {
                log.error("Can't get user " + userId, e);
            }
        }
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public int getUserId() {
        return userId;
    }

    void addResource(Resource resource) {
        resources.add(resource);
    }

    public List<Resource> getResources() {
        return resources;
    }

    /**
     * The survey that grades this submission.
     */
    public int getSurveyResourceId() {
        return surveyResourceId;
    }

    public boolean getSubmitStatus() {
        return submitStatus;
    }

    public void setSubmitStatus(boolean submitStatus) {
        this.submitStatus = submitStatus;
    }
}
