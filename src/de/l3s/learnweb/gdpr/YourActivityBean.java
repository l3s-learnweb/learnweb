package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.User;

/**
 * YourActivityBean is responsible for displaying user activity on site.
 */
@Named
@ViewScoped
public class YourActivityBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -53694900500236594L;
    private static final Logger log = LogManager.getLogger(YourActivityBean.class);

    private List<LogEntry> userActions;
    private Map<Integer, String> groupTitles;

    @Inject
    private GroupDao groupDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        groupTitles = new HashMap<>();

        this.userActions = dao().getLogDao().findAllByUserId(user.getId());
        for (LogEntry action : userActions) {
            try {

                switch (action.getGroupId()) {
                    // general action, which has no group assigned
                    case 0:
                        groupTitles.put(action.getGroupId(), "");
                        break;
                    default:
                        groupTitles.put(action.getGroupId(), groupDao.findById(action.getGroupId()).getTitle());
                        break;
                }
            } catch (Throwable e) {
                log.error("Can't process action '{}' in group {}", action.getAction(), action.getGroupId(), e);
            }
        }
    }

    public List<LogEntry> getUserActions() {
        return userActions;
    }

    public Map<Integer, String> getGroupTitles() {
        return groupTitles;
    }

    /**
     * Omit underscores for frontend without modifying Action class.
     */
    public String getAction(String action) {
        return action.replace("_", " ");
    }
}
