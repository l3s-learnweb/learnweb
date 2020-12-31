package de.l3s.learnweb.group;

import java.io.Serializable;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupOptionsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 7748993079932830367L;
    //private static final Logger log = LogManager.getLogger(GroupOptionsBean.class);

    private int groupId;
    private Group group;
    private int editedGroupLeaderId;
    private int selectedResourceTargetGroupId;

    @NotBlank
    @Length(min = 3, max = 60)
    private String editedGroupTitle;
    @Length(max = 500)
    private String editedGroupDescription; // Group edit fields (Required for editing group)
    private String editedHypothesisLink;
    private String editedHypothesisToken;
    private GroupUser groupUser;

    @Inject
    private GroupDao groupDao;

    public void onLoad() {
        User user = getUser();
        BeanAssert.authorized(user);

        group = groupDao.findByIdOrElseThrow(groupId);

        group.setLastVisit(user);
        editedGroupDescription = group.getDescription();
        editedGroupLeaderId = group.getLeaderUserId();
        editedGroupTitle = group.getTitle();
        editedHypothesisLink = group.getHypothesisLink();
        editedHypothesisToken = group.getHypothesisToken();
        groupUser = groupDao.findGroupUserRelation(group, user).orElseThrow(BeanAssert.NOT_FOUND);
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public void onGroupEdit() {
        if (!StringUtils.equals(editedGroupDescription, group.getDescription())) {
            group.setDescription(editedGroupDescription);
            log(Action.group_changing_description, group.getId(), group.getId());
        }
        if (!editedGroupTitle.equals(group.getTitle())) {
            log(Action.group_changing_title, group.getId(), group.getId(), group.getTitle());
            group.setTitle(editedGroupTitle);
        }
        if (editedGroupLeaderId != group.getLeaderUserId()) {
            if (group.getLeaderUserId() == getUser().getId() || editedGroupLeaderId == getUser().getId()) {
                getUserBean().setSidebarMenuModel(null);
            }

            group.setLeaderUserId(editedGroupLeaderId);
            log(Action.group_changing_leader, group.getId(), group.getId());
        }
        if (!StringUtils.equals(editedHypothesisLink, group.getHypothesisLink())) {
            log(Action.group_changing_hypothesis_link, group.getId(), group.getId(), group.getHypothesisLink());
            group.setHypothesisLink(editedHypothesisLink);
        }
        if (!StringUtils.equals(editedHypothesisToken, group.getHypothesisToken())) {
            group.setHypothesisToken(editedHypothesisToken);
        }
        groupDao.save(group);
        //getLearnweb().getGroupManager().resetCache();
        getUser().clearCaches();

        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void copyGroup() {
        group.copyResources(selectedResourceTargetGroupId, getUser());
        addGrowl(FacesMessage.SEVERITY_INFO, "Copied Resources");
    }

    public List<Group> getUserCopyableGroups() {
        List<Group> copyableGroups = getUser().getWriteAbleGroups();
        copyableGroups.remove(group);
        return copyableGroups;
    }

    public String getEditedGroupTitle() {
        return editedGroupTitle;
    }

    public void setEditedGroupTitle(String editedGroupTitle) {
        this.editedGroupTitle = editedGroupTitle;
    }

    public String getEditedGroupDescription() {
        return editedGroupDescription;
    }

    public void setEditedGroupDescription(String editedGroupDescription) {
        this.editedGroupDescription = editedGroupDescription;
    }

    public int getEditedGroupLeaderId() {
        return editedGroupLeaderId;
    }

    public void setEditedGroupLeaderId(int editedGroupLeaderId) {
        this.editedGroupLeaderId = editedGroupLeaderId;
    }

    public boolean isHypothesisEnabled() {
        return getGroup().getCourse().getOption(Option.Groups_Hypothesis_enabled);
    }

    public String getNewHypothesisLink() {
        return editedHypothesisLink;
    }

    public void setNewHypothesisLink(String newHypothesisLink) {
        this.editedHypothesisLink = newHypothesisLink;
    }

    public String getNewHypothesisToken() {
        return editedHypothesisToken;
    }

    public void setNewHypothesisToken(String newHypothesisToken) {
        this.editedHypothesisToken = newHypothesisToken;
    }

    public int getSelectedResourceTargetGroupId() {
        return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId) {
        this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public GroupUser getGroupUser() {
        return groupUser;
    }
}
