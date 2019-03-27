package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

@Named
@SessionScoped
public class CommonDashboardUserBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = 9047964884484786815L;

    private List<Integer> selectedUsersIds;

    protected Date startDate = null;
    protected Date endDate = null;

    // caches:
    private transient List<Group> allGroups;
    private transient List<User> allUsers;

    public CommonDashboardUserBean()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -6); // load data from last 3 month until now
        startDate = new Date(cal.getTimeInMillis());
        endDate = new Date(new Date().getTime());
    }

    public void setSelectedUsersIds(List<Integer> selectedUsersIds)
    {
        this.selectedUsersIds = selectedUsersIds;
    }

    public List<Integer> getSelectedUsersIds()
    {
        return selectedUsersIds;
    }

    /**
     *
     * @return all groups the current user can moderate
     * @throws SQLException
     */
    public List<Group> getAllGroups() throws SQLException
    {
        if(null == allGroups)
            allGroups = getUser().getOrganisation().getGroups();
        return allGroups;
    }

    /**
     *
     * @return all users the current user can moderate
     * @throws SQLException
     */
    public List<User> getAllUsers() throws SQLException
    {
        if(null == allUsers)
            allUsers = getUser().getOrganisation().getUsers();
        return allUsers;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }
    public void onSubmitSelectedUsers() throws SQLException
    {

    }
}
