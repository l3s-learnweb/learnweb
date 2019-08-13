package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;

// TODO: remove it later (merged with GroupOverviewBean)
@Named
@ViewScoped
public class GroupMembersBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -235626744365943867L;
    //private static final Logger log = Logger.getLogger(GroupMembersBean.class);

    private int groupId;
    private Group group;

    private User clickedUser;
    private List<User> members;

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

    public User getClickedUser()
    {
        return clickedUser;
    }

    public void setClickedUser(User clickedUser)
    {
        this.clickedUser = clickedUser;
    }

    public boolean isMember() throws SQLException
    {
        User user = getUser();

        if(null == user)
            return false;

        if(null == group)
            return false;

        return group.isMember(user);
    }

    public List<User> getMembers() throws SQLException
    {
        if(null == members && group != null)
        {
            members = group.getMembers();
        }
        return members;
    }

    public boolean isUserDetailsHidden()
    {
        User user = getUser();
        if(user == null)
            return false;
        if(user.getOrganisation().getId() == 1249 && user.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames))
            return true;
        return false;
    }
}
