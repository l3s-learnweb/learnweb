package de.l3s.learnweb.dashboard;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public abstract class CommonDashboardUserBean extends ApplicationBean {
    private static final int USERS_LIMIT = 500;

    private Integer paramUserId;

    private int selectedType = 1; // Users = 1, Groups = 2
    private boolean singleUser = true;
    private boolean usersLimitReached = false;
    private List<Integer> selectedUsersIds;
    private List<Integer> selectedGroupsIds;
    protected Date startDate;
    protected Date endDate;

    // caches:
    private transient List<Group> allGroups;
    private transient List<User> allUsers;

    public CommonDashboardUserBean() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -5); // load data from last 5 years until now

        startDate = new Date(cal.getTimeInMillis());
        endDate = new Date();
    }

    public void onLoad() throws SQLException {
        User loggedInUser = getUser(); // the current user
        BeanAssert.authorized(loggedInUser);

        if (!loggedInUser.isModerator()) { // can see only their own statistic
            singleUser = true;
            selectedUsersIds = Collections.singletonList(loggedInUser.getId());
        } else if (paramUserId != null) { // statistic for one user from parameter
            singleUser = true;
            User user = Learnweb.getInstance().getUserManager().getUser(paramUserId);
            BeanAssert.isFound(user);
            selectedUsersIds = Collections.singletonList(user.getId());
            BeanAssert.hasPermission(loggedInUser.canModerateUser(user));
        } else {
            singleUser = false;
        }
    }

    public abstract void cleanAndUpdateStoredData() throws SQLException;

    /**
     * @return all groups the current user can moderate
     */
    public List<Group> getAllGroups() throws SQLException {
        if (null == allGroups) {
            allGroups = getUser().getOrganisation().getGroups();
        }
        return allGroups;
    }

    /**
     * @return all users the current user can moderate
     */
    public List<User> getAllUsers() throws SQLException {
        if (null == allUsers) {
            allUsers = getUser().getOrganisation().getUsers();
        }
        return allUsers;
    }

    public Integer getParamUserId() {
        return paramUserId;
    }

    public void setParamUserId(final Integer paramUserId) {
        this.paramUserId = paramUserId;
    }

    public boolean isSingleUser() {
        return singleUser;
    }

    public boolean isUsersLimitReached() {
        return usersLimitReached;
    }

    public int getSelectedType() {
        return selectedType;
    }

    public void setSelectedType(final int selectedType) {
        this.selectedType = selectedType;
        // reset selected user / group on type change
        this.selectedGroupsIds = null;
        this.selectedUsersIds = null;
    }

    public List<Integer> getSelectedUsersIds() {
        return selectedUsersIds;
    }

    public void setSelectedUsersIds(List<Integer> selectedUsersIds) {
        if (selectedUsersIds.size() > USERS_LIMIT) {
            usersLimitReached = true;
            addMessage(FacesMessage.SEVERITY_ERROR, "Please, choose less than 500 users");
        } else {
            usersLimitReached = false;
        }

        this.selectedUsersIds = selectedUsersIds;
    }

    public User getFirstSelectedUser() throws SQLException {
        if (CollectionUtils.isEmpty(selectedUsersIds)) {
            return null;
        }

        return Learnweb.getInstance().getUserManager().getUser(selectedUsersIds.get(0));
    }

    public List<Integer> getSelectedGroupsIds() {
        return selectedGroupsIds;
    }

    public void setSelectedGroupsIds(final List<Integer> selectedGroupsIds) throws SQLException {
        this.selectedGroupsIds = selectedGroupsIds;
        Set<Integer> selectedUsers = new TreeSet<>();
        for (Integer groupId : selectedGroupsIds) {
            Group group = Learnweb.getInstance().getGroupManager().getGroupById(groupId);
            for (User user : group.getMembers()) {
                selectedUsers.add(user.getId());
            }
        }
        this.setSelectedUsersIds(new ArrayList<>(selectedUsers));
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
