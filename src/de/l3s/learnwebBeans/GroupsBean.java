package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.User;

@ManagedBean
@ViewScoped
public class GroupsBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = 5364340827474357098L;
    private List<Group> joinAbleGroups;
    private List<Group> myGroups;
    private Group selectedGroup;
    private Group editGroup;
    private String selectedGroupDescription;
    private String selectedGroupTitle;
    private int selectedGroupLeaderId;
    private User selectedGroupLeader;
    private int editLeaderId;

    private Group newGroup;

    public GroupsBean() throws SQLException
    {
	if(getUser() == null)
	    return;

	joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());
	myGroups = getUser().getGroups();

	newGroup = new Group();
    }

    public void joinGroup() throws Exception
    {
	System.out.println("joinGroup" + selectedGroup);
	if(null == getUser() || null == selectedGroup)
	    return;

	getUser().joinGroup(selectedGroup);
	myGroups = getUser().getGroups();
	joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());
	log(Action.group_joining, selectedGroup.getId());
	addGrowl(FacesMessage.SEVERITY_INFO, "groupJoined", selectedGroup.getTitle());
    }

    public void leaveGroup() throws Exception
    {
	if(null == getUser() || null == selectedGroup)
	    return;

	getUser().setActiveGroup(selectedGroup);
	log(Action.group_leaving, selectedGroup.getId());

	getUser().leaveGroup(selectedGroup);
	myGroups = getUser().getGroups();
	joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());

	addGrowl(FacesMessage.SEVERITY_INFO, "groupLeft", selectedGroup.getTitle());
    }

    public void deleteGroup() throws Exception
    {
	if(selectedGroup == null)
	    System.err.println("selectedGroup is null");

	if(!canDeleteGroup(selectedGroup))
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "invalid_request");
	    return;
	}
	getUser().setActiveGroup(selectedGroup);
	log(Action.group_deleting, selectedGroup.getId(), selectedGroup.getTitle());

	getUser().deleteGroup(selectedGroup);
	myGroups = getUser().getGroups();
	joinAbleGroups = getLearnweb().getGroupManager().getJoinAbleGroups(getUser());

	Group parentGroup = selectedGroup.getParentGroup();
	if(null != parentGroup)
	    parentGroup.clearCaches();

	addGrowl(FacesMessage.SEVERITY_INFO, "group deleted", selectedGroup.getTitle());
    }

    public boolean canDeleteGroup(Group group) throws SQLException
    {
	User user = getUser();

	if(null == getUser() || null == group)
	    return false;

	if(user.isAdmin() || user.isModerator() || group.isLeader(user))
	    return true;

	return false;
    }

    public String onCreateGroup() throws Exception
    {
	if(null == getUser())
	    return null;

	newGroup.setLeader(getUser());

	Group group = getLearnweb().getGroupManager().save(newGroup);
	getUser().joinGroup(group);

	//getUser().createGroup(newGroup);

	// refresh group list
	myGroups = getUser().getGroups();

	// log and show notification
	log(Action.group_creating, group.getId());
	addGrowl(FacesMessage.SEVERITY_INFO, "groupCreated", newGroup.getTitle());

	// reset new group var
	newGroup = new Group();

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

    public void onGroupEdit()
    {
	System.out.println("onGroupEdit");

	if(null == editGroup)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
	    return;
	}

	try
	{
	    getUser().setActiveGroup(editGroup);

	    if(!selectedGroupDescription.equals(editGroup.getDescription()))
	    {
		editGroup.setDescription(selectedGroupDescription);
		log(Action.group_changing_description, editGroup.getId());
	    }
	    if(!selectedGroupTitle.equals(editGroup.getTitle()))
	    {
		log(Action.group_changing_title, editGroup.getId(), editGroup.getTitle());
		editGroup.setTitle(selectedGroupTitle);
	    }
	    if(selectedGroupLeaderId != editGroup.getLeaderUserId())
	    {
		editGroup.setLeaderUserId(selectedGroupLeaderId);
		log(Action.group_changing_leader, editGroup.getId());
	    }
	    getLearnweb().getGroupManager().save(editGroup);
	    //getLearnweb().getGroupManager().resetCache();
	    getUser().clearCaches();
	    myGroups = getUser().getGroups();

	}
	catch(SQLException e)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
	    e.printStackTrace();
	}

	addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public Group getSelectedGroup()
    {
	return selectedGroup;
    }

    public void setSelectedGroup(Group selectedGroup) throws SQLException
    {
	System.out.println("setselected group");
	System.out.println(selectedGroup);

	this.selectedGroup = selectedGroup;
	this.selectedGroupDescription = selectedGroup.getDescription();
	this.selectedGroupTitle = selectedGroup.getTitle();
	this.selectedGroupLeaderId = selectedGroup.getLeader().getId();
	this.editLeaderId = selectedGroup.getLeader().getId();
	this.selectedGroupLeader = selectedGroup.getLeader();
    }

    public String getSelectedGroupTitle()
    {
	return selectedGroupTitle;
    }

    public void setSelectedGroupTitle(String selectedGroupTitle)
    {
	this.selectedGroupTitle = selectedGroupTitle;
    }

    public int getSelectedGroupLeaderId()
    {
	return selectedGroupLeaderId;
    }

    public void setSelectedGroupLeaderId(int selectedGroupLeaderId)
    {
	this.selectedGroupLeaderId = selectedGroupLeaderId;
    }

    public String getSelectedGroupDescription()
    {
	return selectedGroupDescription;
    }

    public void setSelectedGroupDescription(String selectedGroupDescription)
    {
	this.selectedGroupDescription = selectedGroupDescription;
    }

    public User getSelectedGroupLeader()
    {
	return selectedGroupLeader;
    }

    public void setSelectedGroupLeader(User selectedGroupLeader)
    {
	this.selectedGroupLeader = selectedGroupLeader;
    }

    public Group getEditGroup()
    {
	return editGroup;
    }

    public void setEditGroup(Group editGroup)
    {
	this.editGroup = editGroup;
    }

    public int getEditLeaderId()
    {
	return editLeaderId;
    }

    public void setEditLeaderId(int editLeaderId)
    {
	this.editLeaderId = editLeaderId;
    }

    public List<SelectItem> getMembersOfSelectedGroup() throws SQLException
    {
	if(null == selectedGroup)
	    return new ArrayList<SelectItem>();

	List<SelectItem> yourList;
	yourList = new ArrayList<SelectItem>();

	for(User member : selectedGroup.getMembers())
	    yourList.add(new SelectItem(member.getId(), member.getUsername()));

	return yourList;
    }

}
