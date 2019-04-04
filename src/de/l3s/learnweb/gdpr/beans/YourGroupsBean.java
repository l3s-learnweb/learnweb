package de.l3s.learnweb.gdpr;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;


/**
 * YourGroupsBean is responsible for displaying user group list.
 */
@Named
@ViewScoped
public class YourGroupsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -4009762445275495961L;
    private static final Logger log = Logger.getLogger(YourGroupsBean.class);

    private List<Group> userGroups;

    public YourGroupsBean() throws SQLException
    {
        User user = getUser();
        if(null == user)
            // when not logged in
            return;

        this.userGroups = user.getGroups();
    }

    public List<Group> getUserGroups()
    {
        return this.userGroups;
    }
}
