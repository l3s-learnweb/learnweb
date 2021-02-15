package de.l3s.learnweb.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

public abstract class CommonDashboardUserBean extends ApplicationBean {
    private static final int USERS_LIMIT = 500;

    private Integer paramUserId;

    private int selectedType = 1; // Users = 1, Groups = 2
    private boolean singleUser = true;
    private boolean usersLimitReached = false;
    private List<Integer> selectedUsersIds;
    private List<Integer> selectedGroupsIds;
    protected LocalDate startDate;
    protected LocalDate endDate;

    // caches:
    private transient List<Group> allGroups;
    private transient List<User> allUsers;

    @Inject
    private GroupDao groupDao;

    @Inject
    private UserDao userDao;

    public CommonDashboardUserBean() {
        startDate = LocalDate.now().minusYears(1);
        endDate = LocalDate.now();
    }

    public void onLoad() {
        User loggedInUser = getUser(); // the current user
        BeanAssert.authorized(loggedInUser);

        if (!loggedInUser.isModerator()) { // can see only their own statistic
            singleUser = true;
            selectedUsersIds = Collections.singletonList(loggedInUser.getId());
        } else if (paramUserId != null) { // statistic for one user from parameter
            singleUser = true;
            User user = userDao.findById(paramUserId);
            BeanAssert.isFound(user);
            selectedUsersIds = Collections.singletonList(user.getId());
            BeanAssert.hasPermission(loggedInUser.canModerateUser(user));
        } else {
            singleUser = false;
        }
    }

    public abstract void cleanAndUpdateStoredData();

    /**
     * @return all groups the current user can moderate
     */
    public List<Group> getAllGroups() {
        if (null == allGroups) {
            allGroups = getUser().getOrganisation().getGroups();
        }
        return allGroups;
    }

    /**
     * @return all users the current user can moderate
     */
    public List<User> getAllUsers() {
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

    public User getFirstSelectedUser() {
        if (CollectionUtils.isEmpty(selectedUsersIds)) {
            return null;
        }

        return userDao.findById(selectedUsersIds.get(0));
    }

    public List<Integer> getSelectedGroupsIds() {
        return selectedGroupsIds;
    }

    public void setSelectedGroupsIds(final List<Integer> selectedGroupsIds) {
        this.selectedGroupsIds = selectedGroupsIds;
        Set<Integer> selectedUsers = new TreeSet<>();
        for (Integer groupId : selectedGroupsIds) {
            Group group = groupDao.findById(groupId);
            for (User user : group.getMembers()) {
                selectedUsers.add(user.getId());
            }
        }
        this.setSelectedUsersIds(new ArrayList<>(selectedUsers));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
