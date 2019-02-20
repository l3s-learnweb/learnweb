package de.l3s.learnweb.yourinformation;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogEntry;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

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
    private final static Action[] filter = new Action[] {
            Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document,
            Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader, Action.group_changing_restriction,
            Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving,
            Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.changing_office_resource,
            Action.group_removing_resource };

    public YourActivityBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.userActions = getLearnweb().getLogManager().getLogsByUser(user.getId(), filter, 1000);
        for(LogEntry action: userActions){
            action.setDescription(Jsoup.parse(action.getDescription()).text());
        }
    }

    public List<LogEntry> getUserActions()
    {
        return userActions;
    }
}
