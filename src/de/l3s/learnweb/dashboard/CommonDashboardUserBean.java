package de.l3s.learnweb.dashboard;

import java.sql.SQLException;
import java.util.*;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;
import de.l3s.util.Misc;

public abstract class CommonDashboardUserBean extends ApplicationBean
{
    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";

    private Integer paramUserId;

    private int selectedType = 1; // Users = 1, Groups = 2
    private boolean singleUser = true;
    private List<Integer> selectedUsersIds;
    private List<Integer> selectedGroupsIds;
    protected Date startDate;
    protected Date endDate;

    // caches:
    private transient List<Group> allGroups;
    private transient List<User> allUsers;

    public CommonDashboardUserBean()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -6); // load data from last 6 month until now

        String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
        String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));

        startDate = new Date(Long.parseLong(savedStartDate));
        endDate = new Date(Long.parseLong(savedEndDate));
    }

    public void onLoad()
    {
        User user = getUser(); // the current user
        if(user == null) // not logged in or no privileges
            return;

        if(!user.isModerator()) { // can see only their own statistic
            singleUser = true;
            selectedUsersIds = Collections.singletonList(user.getId());
        } else if (paramUserId != null) { // statistic for one user from parameter
            try
            {
                singleUser = true;
                user = Learnweb.getInstance().getUserManager().getUser(paramUserId);
                selectedUsersIds = Collections.singletonList(user.getId());
            }
            catch(SQLException e)
            {
                throw new RuntimeException("User not found.");
            }
        } else {
            singleUser = false;
        }
    }

    public abstract void cleanAndUpdateStoredData() throws SQLException;

    /**
     * @return all groups the current user can moderate
     */
    public List<Group> getAllGroups() throws SQLException
    {
        if(null == allGroups)
            allGroups = getUser().getOrganisation().getGroups();
        return allGroups;
    }

    /**
     * @return all users the current user can moderate
     */
    public List<User> getAllUsers() throws SQLException
    {
        if(null == allUsers)
            allUsers = getUser().getOrganisation().getUsers();
        return allUsers;
    }

    public Integer getParamUserId()
    {
        return paramUserId;
    }

    public void setParamUserId(final Integer paramUserId)
    {
        this.paramUserId = paramUserId;
    }

    public boolean isSingleUser()
    {
        return singleUser;
    }

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

    public User getFirstSelectedUser() throws SQLException
    {
        if (Misc.nullOrEmpty(selectedUsersIds)) {
            return null;
        }

        return Learnweb.getInstance().getUserManager().getUser(selectedUsersIds.get(0));
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

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
        setPreference(PREFERENCE_STARTDATE, Long.toString(startDate.getTime()));
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
        setPreference(PREFERENCE_ENDDATE, Long.toString(endDate.getTime()));
    }
}
