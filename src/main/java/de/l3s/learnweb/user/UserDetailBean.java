package de.l3s.learnweb.user;

import java.util.EnumSet;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.Organisation.Option;

@Named
@RequestScoped
public class UserDetailBean extends ApplicationBean {
    //private static final Logger log = LogManager.getLogger(UserDetailBean.class);

    private static final EnumSet<Action> USER_ACTIONS = EnumSet.of(Action.register, Action.adding_resource, Action.commenting_resource, Action.edit_resource,
        Action.deleting_resource, Action.group_changing_description, Action.group_changing_leader, Action.group_changing_restriction,
        Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving,
        Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.changing_office_resource);

    private int userId;
    private User selectedUser;
    private boolean pageHidden = false; // true when the course uses username anonymization
    private List<LogEntry> latestLogEntries;

    @Inject
    private UserDao userDao;

    public void onLoad() {
        User loggedInUser = getUser();
        BeanAssert.authorized(loggedInUser);

        if (userId == 0 || loggedInUser.getId() == userId) {
            selectedUser = loggedInUser; // user edits himself
        } else {
            selectedUser = userDao.findByIdOrElseThrow(userId); // an admin views a user
        }

        if (selectedUser.getOrganisation().getOption(Option.Privacy_Anonymize_usernames)) {
            pageHidden = true;
        }

        latestLogEntries = dao().getLogDao().findPublicByUserId(selectedUser.getId(), Action.collectOrdinals(USER_ACTIONS), 50);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    /**
     * true when the course uses username anonymization.
     */
    public boolean isPageHidden() {
        return pageHidden;
    }

    public List<LogEntry> getLatestLogEntries() {
        return latestLogEntries;
    }

}
