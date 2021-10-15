package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

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
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupsBean extends ApplicationBean implements Serializable {
    private static final Logger log = LogManager.getLogger(GroupsBean.class);
    @Serial
    private static final long serialVersionUID = 5364340827474357098L;

    private List<Group> joinAbleGroups;
    private List<Group> myGroups;
    private Group selectedGroup;

    private Group newGroup;
    private List<Course> editAbleCourses; // courses to which the user can add groups to

    @Inject
    private GroupDao groupDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        joinAbleGroups = groupDao.findJoinAble(getUser());
        myGroups = user.getGroups();
        newGroup = new Group();

        editAbleCourses = user.getCourses().stream().filter(course -> !course.getOption(Option.Groups_Only_moderators_can_create_groups)).collect(Collectors.toList());
    }

    public void joinGroup() {
        User user = getUser();

        if (null == user || null == selectedGroup) {
            return;
        }

        if (selectedGroup.isMemberCountLimited()) {
            if (selectedGroup.getMemberCount() >= selectedGroup.getMaxMemberCount()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "group_full");
                return;
            }
        }

        user.joinGroup(selectedGroup);
        user.setGuide(User.Guide.JOIN_GROUP, true);
        myGroups = getUser().getGroups();
        joinAbleGroups = groupDao.findJoinAble(getUser());
        log(Action.group_joining, selectedGroup.getId(), selectedGroup.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "groupJoined", selectedGroup.getTitle());
    }

    public void leaveGroup() {
        if (null == getUser() || null == selectedGroup) {
            return;
        }

        log(Action.group_leaving, selectedGroup.getId(), selectedGroup.getId());

        getUser().leaveGroup(selectedGroup);
        myGroups = getUser().getGroups();
        joinAbleGroups = groupDao.findJoinAble(getUser());

        addGrowl(FacesMessage.SEVERITY_INFO, "groupLeft", selectedGroup.getTitle());
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

        log(Action.group_deleting, selectedGroup.getId(), selectedGroup.getId(), selectedGroup.getTitle());

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
            newGroup.setCourseId(user.getCourses().get(0).getId());
        }
        groupDao.save(newGroup);
        user.joinGroup(newGroup);
        user.setGuide(User.Guide.JOIN_GROUP, true);
        // refresh group list
        myGroups = user.getGroups();

        // log and show notification
        log(Action.group_creating, newGroup.getId(), newGroup.getId());
        addGrowl(FacesMessage.SEVERITY_INFO, "groupCreated", newGroup.getTitle());

        // reset new group var
        newGroup = new Group();
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

    public List<Course> getEditAbleCourses() {
        return editAbleCourses;
    }
}
