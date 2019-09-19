package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.validator.constraints.Length;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.Course.Option;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupOptionsBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 7748993079932830367L;
    private static final Logger log = Logger.getLogger(GroupOptionsBean.class);

    private int groupId;
    private Group group;

    private int editedGroupLeaderId;
    private int selectedResourceTargetGroupId;

    @NotBlank
    @Length(min = 3, max = 60)
    private String editedGroupTitle;
    private String editedGroupDescription; // Group edit fields (Required for editing group)
    private String editedHypothesisLink;
    private String editedHypothesisToken;

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(null == user) // not logged in
            return;

        group = getLearnweb().getGroupManager().getGroupById(groupId);

        if(null == group)
            addInvalidParameterMessage("group_id");

        if(null != group)
        {
            user.setActiveGroup(group);
            group.setLastVisit(user);
        }

        if(group != null)
        {
            editedGroupDescription = group.getDescription();
            editedGroupLeaderId = group.getLeaderUserId();
            editedGroupTitle = group.getTitle();
            editedHypothesisLink = group.getHypothesisLink();
            editedHypothesisToken = group.getHypothesisToken();
        }
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public Group getGroup()
    {
        return group;
    }

    public void onGroupEdit()
    {
        if(null == group)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
            return;
        }

        try
        {
            getUser().setActiveGroup(group);

            if(!editedGroupDescription.equals(group.getDescription()))
            {
                group.setDescription(editedGroupDescription);
                log(Action.group_changing_description, group.getId(), group.getId());
            }
            if(!editedGroupTitle.equals(group.getTitle()))
            {
                log(Action.group_changing_title, group.getId(), group.getId(), group.getTitle());
                group.setTitle(editedGroupTitle);
            }
            if(editedGroupLeaderId != group.getLeaderUserId())
            {
                group.setLeaderUserId(editedGroupLeaderId);
                log(Action.group_changing_leader, group.getId(), group.getId());
            }
            if(!StringUtils.equals(editedHypothesisLink, group.getHypothesisLink()))
            {
                log(Action.group_changing_hypothesis_link, group.getId(), group.getId(), group.getHypothesisLink());
                group.setHypothesisLink(editedHypothesisLink);
            }
            if(!StringUtils.equals(editedHypothesisToken, group.getHypothesisToken()))
            {
                group.setHypothesisToken(editedHypothesisToken);
            }
            getLearnweb().getGroupManager().save(group);
            //getLearnweb().getGroupManager().resetCache();
            getUser().clearCaches();

        }
        catch(SQLException e)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
            log.error("unhandled error", e);
        }

        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void copyGroup()
    {

        if(null == group)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
            return;
        }

        try
        {
            group.copyResourcesToGroupById(selectedResourceTargetGroupId, getUser());
        }
        catch(SQLException e)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
            log.error("unhandled error", e);
        }
        addGrowl(FacesMessage.SEVERITY_INFO, "Copied Resources");
    }

    public List<Group> getUserCopyableGroups() throws SQLException
    {
        List<Group> copyableGroups = getUser().getWriteAbleGroups();
        copyableGroups.remove(group);
        return copyableGroups;
    }

    public String getEditedGroupTitle()
    {
        return editedGroupTitle;
    }

    public void setEditedGroupTitle(String editedGroupTitle)
    {
        this.editedGroupTitle = editedGroupTitle;
    }

    public String getEditedGroupDescription()
    {
        return editedGroupDescription;
    }

    public void setEditedGroupDescription(String editedGroupDescription)
    {
        this.editedGroupDescription = editedGroupDescription;
    }

    public int getEditedGroupLeaderId()
    {
        return editedGroupLeaderId;
    }

    public void setEditedGroupLeaderId(int editedGroupLeaderId)
    {
        this.editedGroupLeaderId = editedGroupLeaderId;
    }

    public List<SelectItem> getMembersSelectItemList() throws SQLException
    {
        if(null == group)
            return new ArrayList<>();

        List<SelectItem> yourList;
        yourList = new ArrayList<>();

        for(User member : group.getMembers())
            yourList.add(new SelectItem(member.getId(), member.getUsername()));

        return yourList;
    }

    public boolean isHypothesisEnabled() throws SQLException
    {
        return getGroup().getCourse().getOption(Option.Groups_Hypothesis_enabled);
    }

    public String getNewHypothesisLink()
    {
        return editedHypothesisLink;
    }

    public void setNewHypothesisLink(String newHypothesisLink)
    {
        this.editedHypothesisLink = newHypothesisLink;
    }

    public String getNewHypothesisToken()
    {
        return editedHypothesisToken;
    }

    public void setNewHypothesisToken(String newHypothesisToken)
    {
        this.editedHypothesisToken = newHypothesisToken;
    }

    public int getSelectedResourceTargetGroupId()
    {
        return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
        this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }
}
