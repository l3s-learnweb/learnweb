package de.l3s.learnweb.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.logging.LogEntry;

@Named
@ViewScoped
public class LogsBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 42220709533639671L;
    //private static final Logger log = LogManager.getLogger(LogsBean.class);

    private static final EnumSet<Action> AVAILABLE_ACTIONS;

    static {
        // list all actions the user shall be able to view
        EnumSet<Action> resourceActions = EnumSet.copyOf(Action.getActionsByCategory(ActionCategory.RESOURCE));
        // remove actions we don't want to show
        // resourceActions.remove(Action.opening_resource);
        // resourceActions.remove(Action.glossary_open);
        resourceActions.remove(Action.lock_interrupted_returned_resource);
        resourceActions.remove(Action.lock_rejected_edit_resource);
        // resourceActions.remove(Action.downloading);
        AVAILABLE_ACTIONS = resourceActions;
    }

    private int userId;
    private User selectedUser;
    private List<LogEntry> logEntries;

    @Inject
    private UserDao userDao;
    private EnumSet<Action> selectedActions = AVAILABLE_ACTIONS; // TODO implement client side selector

    public void onLoad() {
        User loggedInUser = getUser();
        BeanAssert.authorized(loggedInUser);

        if (userId == 0 || loggedInUser.getId() == userId) {
            selectedUser = loggedInUser; // user views himself
        } else {
            selectedUser = userDao.findByIdOrElseThrow(userId); // a moderator views another user

            BeanAssert.hasPermission(getUser().canModerateUser(selectedUser));
        }

        logEntries = dao().getLogDao().findByUserId(selectedUser.getId(), Action.collectOrdinals(selectedActions), 100);
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

    public List<LogEntry> getLogEntries() {
        return logEntries;
    }
}
