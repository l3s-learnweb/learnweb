package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;

@ManagedBean
@ViewScoped
public class GroupsBean extends ApplicationBean implements Serializable
{
    private static final Logger log = Logger.getLogger(GroupsBean.class);
    private static final long serialVersionUID = 5364340827474357098L;

    private List<Group> joinAbleGroups;
    private List<Group> myGroups;
    private Group selectedGroup;

    private Group newGroup;
    private boolean otherGroupsShowLanguage;

    public GroupsBean()
    {
        if(getUser() == null)
            return;

        try
        {
            joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());
            myGroups = getUser().getGroups();
            newGroup = new Group();

            otherGroupsShowLanguage = isLanguageSet(joinAbleGroups);
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void joinGroup()
    {
        try
        {
            User user = getUser();

            if(null == user || null == selectedGroup)
                return;

            // make sure users can not join groups simultaneously
            synchronized(selectedGroup)
            {
                if(selectedGroup.isMemberCountLimited())
                {
                    if(selectedGroup.getMemberCount() >= selectedGroup.getMaxMemberCount())
                    {
                        addMessage(FacesMessage.SEVERITY_ERROR, "group_full");
                        return;
                    }
                }
                user.joinGroup(selectedGroup);
                myGroups = getUser().getGroups();
                joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());
                log(Action.group_joining, selectedGroup.getId(), selectedGroup.getId());
                addGrowl(FacesMessage.SEVERITY_INFO, "groupJoined", selectedGroup.getTitle());
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void leaveGroup()
    {
        try
        {
            if(null == getUser() || null == selectedGroup)
                return;

            log(Action.group_leaving, selectedGroup.getId(), selectedGroup.getId());

            getUser().leaveGroup(selectedGroup);
            myGroups = getUser().getGroups();
            joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());

            addGrowl(FacesMessage.SEVERITY_INFO, "groupLeft", selectedGroup.getTitle());
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public String deleteGroup()
    {
        try
        {
            if(selectedGroup == null)
            {
                log.error("selectedGroup is null");
                return null;
            }

            if(!canDeleteGroup(selectedGroup))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
                return null;
            }

            getUser().setActiveGroup(selectedGroup);
            log(Action.group_deleting, selectedGroup.getId(), selectedGroup.getId(), selectedGroup.getTitle());

            getUser().deleteGroup(selectedGroup);
            myGroups = getUser().getGroups();
            joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());

            addMessage(FacesMessage.SEVERITY_INFO, "group_deleted", selectedGroup.getTitle());

            return "/lw/myhome/groups.xhtml";
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
        return null;
    }

    public boolean canDeleteGroup(Group group) throws SQLException
    {
        if(null == group)
            return false;

        return group.canDeleteGroup(getUser());
    }

    public String onCreateGroup()
    {
        if(null == getUser())
            return null;

        try
        {
            newGroup.setLeader(getUser());

            Group group = getLearnweb().getGroupManager().save(newGroup);
            getUser().joinGroup(group);

            // refresh group list
            myGroups = getUser().getGroups();

            // log and show notification
            log(Action.group_creating, group.getId(), group.getId());
            addGrowl(FacesMessage.SEVERITY_INFO, "groupCreated", newGroup.getTitle());

            // reset new group var
            newGroup = new Group();
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
        return null;
    }

    public void validateGroupTitle(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
        String title = (String) value;

        if(getLearnweb().getGroupManager().getGroupByTitleFilteredByOrganisation(title, getUser().getOrganisationId()) != null)
        {
            throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "title_already_taken"));
        }
    }

    public List<Group> getOtherGroups()
    {
        return joinAbleGroups;
    }

    public List<Group> getMyGroups()
    {
        return myGroups;
    }

    public Group getNewGroup()
    {
        return newGroup;
    }

    public Group getSelectedGroup()
    {
        return selectedGroup;
    }

    public void setSelectedGroup(Group selectedGroup) throws SQLException
    {
        this.selectedGroup = selectedGroup;
    }

    public boolean isOtherGroupsShowLanguage()
    {
        return otherGroupsShowLanguage;
    }

    private boolean isLanguageSet(List<Group> groups)
    {
        for(Group group : groups)
        {
            if(StringUtils.isEmpty(group.getLanguage()))
                return false;
        }
        return true;
    }

}
