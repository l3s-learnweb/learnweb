package de.l3s.learnweb.dashboard;

import java.sql.SQLException;
import java.util.*;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public abstract class CommonDashboardUserBean extends ApplicationBean
{
    private int selectedType = 1;
    private List<Integer> selectedUsersIds;
    private List<Integer> selectedGroupsIds;
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

    public void onLoad()
    {
        User user = getUser(); // the current user
        if(user == null) // not logged in or no privileges
            return;

        if(isReadOnly()) {
            selectedUsersIds = Collections.singletonList(getUser().getId());
        }
    }

    public boolean isReadOnly()
    {
        return !getUser().isModerator();
    }

    public abstract void cleanAndUpdateStoredData() throws SQLException;

    public int getSelectedType()
    {
        return selectedType;
    }

    public void setSelectedType(final int selectedType)
    {
        this.selectedType = selectedType;
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

    public List<Integer> getSelectedGroupsIds()
    {
        return selectedGroupsIds;
    }

    public void setSelectedGroupsIds(final List<Integer> selectedGroupsIds) throws SQLException
    {
        this.selectedGroupsIds = selectedGroupsIds;
        List<Integer> selectedUsers = new ArrayList<>();
        for(Integer groupId : selectedGroupsIds)
        {
            Group group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
            for(User user : group.getMembers())
                selectedUsers.add(user.getId());
        }
        this.setSelectedUsersIds(selectedUsers);
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
