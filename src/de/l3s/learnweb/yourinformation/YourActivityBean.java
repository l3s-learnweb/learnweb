package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YourActivityBean is responsible for displaying user activity on site.
 */
@Named
@ViewScoped
public class YourActivityBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -53694900500236594L;
    private static final Logger log = Logger.getLogger(YourActivityBean.class);

    private List<LogEntry> userActions;
    private Map<Integer, String> groupTitles;

    public YourActivityBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        final GroupManager groupManager = this.getLearnweb().getGroupManager();
        groupTitles = new HashMap<>();

        this.userActions = getLearnweb().getLogManager().getLogsByUser(user.getId(), Action.values(), 1000);
        for(LogEntry action: userActions){
            //action.setDescription(Jsoup.parse(action.getDescription()).text());
            switch(action.getGroupId()){
                // general action which has no group assigned
                case 0:
                    groupTitles.put(action.getGroupId(),"");
                    break;
                default:
                    groupTitles.put(action.getGroupId(), groupManager.getGroupById(action.getGroupId()).getTitle());
                    break;
            }
        }
    }

    public List<LogEntry> getUserActions()
    {
        return userActions;
    }

    public Map<Integer, String> getGroupTitles()
    {
        return groupTitles;
    }
}
