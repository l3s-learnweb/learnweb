package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.resource.submission.Submission;
import de.l3s.learnweb.searchhistory.Pkg;
import de.l3s.util.HasId;
import de.l3s.util.StringHelper;

@Named
@RequestScoped
public class WelcomeBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -4337683111157393180L;

    // Filter for forum
    private static final EnumSet<Action> FORUM_FILTER = EnumSet.of(
        Action.forum_post_added,
        Action.forum_topic_added
    );

    // Filter for resources
    private static final EnumSet<Action> RESOURCES_FILTER = EnumSet.of(
        Action.adding_resource,
        Action.edit_resource
    );

    private static final EnumSet<Action> GENERAL_FILTER = EnumSet.of(
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
    );

    private String organisationWelcomeMessage;

    // TODO @kemkes: this separation is useless, all log entries are already included in the general logs; It needs to be discussed what we shall show here
    private List<LogEntry> newsForums;
    private List<LogEntry> newsResources;
    private List<LogEntry> newsGeneral;
    private List<Message> receivedMessages;
    private boolean hideNewsPanel; // true when there is nothing to show in the news tabs

    private List<Course> coursesWithWelcomeMessage;

    private List<Submission> activeSubmissions;

    @Inject
    private MessageDao messageDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        receivedMessages = messageDao.findIncoming(getUser(), 5);

        if (getUser().getGroupCount() == 0) {
            hideNewsPanel = true;
        } else {
            newsGeneral = getLogs(GENERAL_FILTER, 20);
            newsResources = getLogs(RESOURCES_FILTER, 5);
            newsForums = getLogs(FORUM_FILTER, 5);

            hideNewsPanel = newsResources.isEmpty() && newsForums.isEmpty() && receivedMessages.isEmpty();
        }

        if (!StringHelper.isBlankDisregardingHTML(user.getOrganisation().getWelcomeMessage())) {
            organisationWelcomeMessage = user.getOrganisation().getWelcomeMessage();
        }

        // retrieve all the users courses whose welcome message isn't blank
        coursesWithWelcomeMessage = user.getCourses().stream()
            .filter(course -> !StringHelper.isBlankDisregardingHTML(course.getWelcomeMessage()))
            .collect(Collectors.toList());

        activeSubmissions = user.getActiveSubmissions();
        //
        List<Group> groups = dao().getGroupDao().findByUserId(user.getId());
        int groupId = 0;
        if (!groups.isEmpty()) groupId = groups.get(0).getId();
        Pkg.instance.createPkg(groupId);

    }

    private List<LogEntry> getLogs(EnumSet<Action> filter, int limit) {
        // ids of all groups the user is member of
        List<Integer> groupIds = HasId.collectIds(getUser().getGroups());
        return dao().getLogDao().findByUsersGroupIds(getUser().getId(), groupIds, Action.collectOrdinals(filter), limit);
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

    public List<Submission> getActiveSubmissions() {
        return activeSubmissions;
    }

    public List<Message> getReceivedMessages() {
        return receivedMessages;
    }

    public boolean isHideNewsPanel() {
        return hideNewsPanel;
    }

}
