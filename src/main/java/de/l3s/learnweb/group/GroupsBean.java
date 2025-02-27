package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.EventBus;
import de.l3s.learnweb.logging.LearnwebGroupEvent;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupsBean extends ApplicationBean implements Serializable {
    private static final Logger log = LogManager.getLogger(GroupsBean.class);
    @Serial
    private static final long serialVersionUID = 5364340827474357098L;

    private transient List<Group> joinAbleGroups;
    private transient List<Group> myGroups;
    private transient Group selectedGroup;

    private transient Group newGroup;

    @Inject
    private GroupDao groupDao;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        joinAbleGroups = groupDao.findJoinAble(getUser());
        myGroups = user.getGroups();
        newGroup = new Group();
    }

    public void joinGroup() {
        User user = getUser();

        if (null == user || null == selectedGroup) {
            return;
        }

        if (selectedGroup.isMemberCountLimited() && (selectedGroup.getMemberCount() >= selectedGroup.getMaxMemberCount())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "group_full");
            return;
        }

        user.joinGroup(selectedGroup);
        myGroups = getUser().getGroups();
        joinAbleGroups = groupDao.findJoinAble(getUser());

        addGrowl(FacesMessage.SEVERITY_INFO, "groupJoined", selectedGroup.getTitle());
        eventBus.dispatch(new LearnwebGroupEvent(Action.group_joining, selectedGroup));
    }

    public void leaveGroup() {
        if (null == getUser() || null == selectedGroup) {
            return;
        }

        getUser().leaveGroup(selectedGroup);
        myGroups = getUser().getGroups();
        joinAbleGroups = groupDao.findJoinAble(getUser());

        addGrowl(FacesMessage.SEVERITY_INFO, "groupLeft", selectedGroup.getTitle());
        eventBus.dispatch(new LearnwebGroupEvent(Action.group_leaving, selectedGroup));
    }

    public String deleteGroup() {
        if (selectedGroup == null) {
            log.error("selectedGroup is null");
            return null;
        }

        if (!canDeleteGroup(selectedGroup)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to delete this group");
            return null;
        }

        eventBus.dispatch(new LearnwebGroupEvent(Action.group_deleting, selectedGroup).setParams(selectedGroup.getTitle()));

        groupDao.deleteSoft(selectedGroup);
        myGroups = getUser().getGroups();
        joinAbleGroups = groupDao.findJoinAble(getUser());

        addMessage(FacesMessage.SEVERITY_INFO, "group_deleted", selectedGroup.getTitle());

        return "/lw/myhome/groups.xhtml";
    }

    public boolean canDeleteGroup(Group group) {
        if (null == group) {
            return false;
        }

        return group.canDeleteGroup(getUser());
    }

    public void onCreateGroup() {
        User user = getUser();
        newGroup.setLeader(user);

        if (newGroup.getCourseId() == 0) { // this happens when the user is only member of a single course and the course selector isn't shown
            newGroup.setCourseId(user.getCourses().getFirst().getId());
        }
        groupDao.save(newGroup);
        user.joinGroup(newGroup);

        // refresh group list & clean cache
        myGroups = user.getGroups();
        getUserBean().setSidebarMenuModel(null);
        newGroup = new Group();

        addGrowl(FacesMessage.SEVERITY_INFO, "groupCreated", newGroup.getTitle());
        eventBus.dispatch(new LearnwebGroupEvent(Action.group_creating, newGroup));
    }

    public void validateGroupTitle(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String title = (String) value;

        if (groupDao.findByTitleAndOrganisationId(title, getUser().getOrganisationId()).isPresent()) {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "title_already_taken"));
        }
    }

    public List<Group> getOtherGroups() {
        return joinAbleGroups;
    }

    public List<Group> getMyGroups() {
        return myGroups;
    }

    public Group getNewGroup() {
        return newGroup;
    }

    public Group getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(Group selectedGroup) {
        this.selectedGroup = selectedGroup;
    }
}
