package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.util.StringHelper;

@Named
@RequestScoped
public class WelcomeBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4337683111157393180L;

    // Filter for forum
    private static final Action[] FORUM_FILTER = {
        Action.forum_post_added,
        Action.forum_topic_added
    };

    // Filter for resources
    private static final Action[] RESOURCES_FILTER = {
        Action.adding_resource,
        Action.edit_resource,
    };

    private static final Action[] GENERAL_FILTER = {
        Action.adding_resource,
        Action.commenting_resource,
        Action.edit_resource,
        Action.group_changing_description,
        Action.group_changing_leader,
        Action.group_changing_title,
        Action.group_creating,
        Action.group_deleting,
        Action.rating_resource,
        Action.tagging_resource,
        Action.thumb_rating_resource,
        Action.forum_topic_added,
        Action.changing_office_resource,
        Action.forum_post_added
    };

    private String organisationWelcomeMessage;

    // TODO: this separation is useless, all log entries are already included in the general logs; It needs to be discussed what we shall show here
    private List<LogEntry> newsForums;
    private List<LogEntry> newsResources;
    private List<LogEntry> newsGeneral;
    private ArrayList<Message> receivedMessages;
    private boolean hideNewsPanel; // true when there is nothing to show in the news tabs

    private List<Course> coursesWithWelcomeMessage;

    private ArrayList<Submission> activeSubmissions;

    public WelcomeBean() {
        User user = getUser();
        if (null == user) {
            return;
        }

        try {
            newsGeneral = getLogs(GENERAL_FILTER, 20);

            newsResources = getLogs(RESOURCES_FILTER, 5);
            newsForums = getLogs(FORUM_FILTER, 5);
            receivedMessages = Message.getAllMessagesToUser(getUser(), 5);

            hideNewsPanel = newsResources.isEmpty() && newsForums.isEmpty() && receivedMessages.isEmpty();

            if (!StringHelper.isBlankDisregardingHTML(user.getOrganisation().getWelcomeMessage())) {
                organisationWelcomeMessage = user.getOrganisation().getWelcomeMessage();
            }

            // retrieve all the users courses whose welcome message isn't blank
            coursesWithWelcomeMessage = user.getCourses().stream().filter(c -> !StringHelper.isBlankDisregardingHTML(c.getWelcomeMessage())).collect(Collectors.toList());

            activeSubmissions = user.getActiveSubmissions();

        } catch (SQLException e) {
            addErrorMessage("An error occurred while loading the content for this page", e);
        }
    }

    private List<LogEntry> getLogs(Action[] filter, int limit) throws SQLException {
        return getLearnweb().getLogManager().getActivityLogOfUserGroups(getUser().getId(), filter, limit);
    }

    public List<LogEntry> getNewsResources() {
        return newsResources;
    }

    public String getOrganisationWelcomeMessage() {
        return organisationWelcomeMessage;
    }

    public List<LogEntry> getNewsForums() {
        return newsForums;
    }

    public List<LogEntry> getNewsGeneral() {
        return newsGeneral;
    }

    public List<Course> getCoursesWithWelcomeMessage() {
        return coursesWithWelcomeMessage;
    }

    public ArrayList<Submission> getActiveSubmissions() {
        return activeSubmissions;
    }

    public ArrayList<Message> getReceivedMessages() {
        return receivedMessages;
    }

    public boolean isHideNewsPanel() {
        return hideNewsPanel;
    }

}
