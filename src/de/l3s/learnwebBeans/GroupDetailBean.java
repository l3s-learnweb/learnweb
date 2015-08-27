package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import de.l3s.learnweb.AbstractPaginator;
import de.l3s.learnweb.Comment;
import de.l3s.learnweb.GoogleDriveManager;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.JForumManager;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Link;
import de.l3s.learnweb.Link.LinkType;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.NewsEntry;
import de.l3s.learnweb.Presentation;
import de.l3s.learnweb.PresentationManager;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceDecorator;
import de.l3s.learnweb.ResourceManager;
import de.l3s.learnweb.ResourceManager.Order;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.util.MD5;

@ManagedBean
@ViewScoped
public class GroupDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;

    private int groupId;
    private Group group;
    private List<User> members;

    private List<Presentation> presentations;

    private String mode = "everything";
    private List<LogEntry> logMessages;
    private ArrayList<NewsEntry> newslist;
    private User clickedUser;
    private Group clickedGroup;

    private Boolean newResourceClicked = false;
    private Boolean editResourceClicked = false;
    private Resource selectedResource;
    public Resource clickedResource;
    public Presentation clickedPresentation;

    private boolean allLogs = false;
    private boolean reloadLogs = false;

    @NotBlank
    private String newLinkUrl;
    @NotBlank
    private String newLinkTitle;
    private String newLinkType;

    private Link selectedLink;
    private Link editLink;

    private List<Link> links; // the same as group.getLinks() but with a link to the forum
    private List<Link> documentLinks;
    private String resourceSorting = "title";

    private boolean isNewestResourceHidden = false;

    private int selectedResourceTargetGroupId;

    private AbstractPaginator paginator;
    private Order order = Order.TITLE;

    private String query;

    public GroupDetailBean() throws SQLException
    {
	loadGroup();

	if(null == group)
	{
	    addMessage(FacesMessage.SEVERITY_ERROR, "invalid group id");
	    return;
	}

	updateLinksList();

	Resource temp = new Resource();
	clickedResource = temp;
	clickedUser = new User(); // TODO initialize with null
	clickedGroup = new Group();// TODO initialize with null
	clickedPresentation = new Presentation();// TODO initialize with null

	paginator = group.getResources(order);
    }

    public void preRenderView(ComponentSystemEvent e)
    {
	User user = getUser();
	if(null != user && null != group)
	{
	    try
	    {
		user.setActiveGroup(group);

		group.setLastVisit(user);
	    }
	    catch(Exception e1)
	    {
		e1.printStackTrace();

		addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	    }
	}
    }

    private void convert()
    {
	HashSet<Integer> deletedResources = new HashSet<Integer>();
	Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
		Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource, Action.group_removing_resource };
	List<LogEntry> feed = logMessages;

	if(feed != null)
	{
	    newslist = new ArrayList<NewsEntry>();
	    for(LogEntry l : feed)
	    {
		User u = null;
		Resource r = null;
		boolean resourceaction = true;
		try
		{
		    u = getLearnweb().getUserManager().getUser(l.getUserId());
		    r = getLearnweb().getResourceManager().getResource(l.getResourceId());
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		}
		if(r != null && deletedResources.contains(r.getId()))
		    resourceaction = false;

		int commentcount = 0;
		int tagcount = 0;
		String text = l.getDescription();
		if(l.getAction() == filter[3] || r == null || l.getAction() == filter[17])
		{
		    if(r != null)
			deletedResources.add(r.getId());
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, !resourceaction, l.getDate()));
		    continue;
		}
		try
		{
		    if(r.getComments() != null)
			commentcount += r.getComments().size();
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		}

		try
		{
		    if(r.getTags() != null)
			tagcount += r.getTags().size();
		}
		catch(Exception e)
		{
		    e.printStackTrace();
		    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		}

		if(l.getAction() == filter[0]) //add_resource
		{

		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[1] && commentcount > 0)
		{
		    Comment commenttobeadded = new Comment();
		    commenttobeadded.setText("comment removed!");
		    try
		    {

			for(Comment c : getLearnweb().getResourceManager().getCommentsByResourceId(r.getId()))
			{
			    if(c.getId() == Integer.parseInt(l.getParams()))
			    {
				commenttobeadded = c;
			    }
			}

		    }
		    catch(SQLException e)
		    {
			e.printStackTrace();
			addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
		    }
		    text = text + " with " + "<b>" + commenttobeadded.getText() + "</b>";
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[15])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}
		if(l.getAction() == filter[14])
		{
		    newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));
		    continue;

		}

		newslist.add(new NewsEntry(l, u, r, commentcount, tagcount, text, resourceaction, l.getDate()));

	    }

	}

    }

    public ArrayList<NewsEntry> getNewslist()
    {
	if(null == newslist || reloadLogs)
	{
	    loadLogs(25);

	    if(newslist.size() < 25)
		allLogs = true;
	}

	return newslist;

    }

    public void setNewslist(ArrayList<NewsEntry> newslist)
    {
	this.newslist = newslist;
    }

    public String present()
    {
	return "presentation?id=" + clickedPresentation.getPresentationId() + "&faces-redirect=true";
    }

    public String editPresentation(String format)
    {
	return "../lw/myhome/reedit_presentation.jsf?group_id=" + groupId + "&presentation_id=" + clickedPresentation.getPresentationId() + "&format=" + format;
    }

    public String edit(String format)
    {
	return "../lw/myhome/edit_presentation.jsf?group_id=" + groupId + "&format=" + format;
    }

    public String getMode()
    {
	return mode;
    }

    private void loadGroup() throws SQLException
    {
	if(0 == groupId)
	{/*
	    String temp = getFacesContext().getExternalContext().getRequestParameterMap().get("group_id");
	    if(temp != null && temp.length() != 0)
	 groupId = Integer.parseInt(temp);

	    if(0 == groupId)
	 return;
	   */
	    Integer id = getParameterInt("group_id");

	    if(null == id)
		return;

	    groupId = id.intValue();
	}

	group = getLearnweb().getGroupManager().getGroupById(groupId);
    }

    private void loadLogs(Integer limit)
    {
	try
	{
	    if(limit != null)
		logMessages = getLearnweb().getLogsByGroup(groupId, null, limit);
	    else
		logMessages = getLearnweb().getLogsByGroup(groupId, null);
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
	convert();

    }

    private void updateLinksList() throws SQLException
    {
	documentLinks = group.getDocumentLinks();
	links = new LinkedList<Link>(group.getLinks());
	String forumUrl = group.getForumUrl(getUser());
	if(forumUrl != null)
	    links.add(0, new Link(Link.LinkType.LINK, getLocaleMessage("forum"), forumUrl));
    }

    public List<User> getMembers() throws SQLException
    {
	if(null == members)
	{
	    loadGroup();

	    if(null == group)
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "Missing or wrong parameter: group_id");
		return null;
	    }
	    members = group.getMembers();
	}
	return members;
    }

    public Group getGroup() throws SQLException
    {
	loadGroup();
	return group;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public List<LogEntry> getLogMessages() throws SQLException
    {
	if(null == logMessages)
	{
	    logMessages = getLearnweb().getLogsByGroup(groupId, null);
	    convert();
	}
	return logMessages;
    }

    public void fetchAllLogs()
    {
	setAllLogs(true);
	loadLogs(null);
    }

    public Resource getSelectedResource()
    {
	return selectedResource;
    }

    public void setSelectedResource(Resource selectedResource)
    {
	this.selectedResource = selectedResource;
    }

    public String getNewLinkUrl()
    {
	return newLinkUrl;
    }

    public void setNewLinkUrl(String newLinkUrl)
    {
	this.newLinkUrl = newLinkUrl;
    }

    public String getNewLinkTitle()
    {
	return newLinkTitle;
    }

    public void setNewLinkTitle(String newLinkTitle)
    {
	this.newLinkTitle = newLinkTitle;
    }

    public String getNewLinkType()
    {
	return newLinkType;
    }

    public void setNewLinkType(String newLinkType)
    {
	this.newLinkType = newLinkType;
    }

    public List<Link> getDocumentLinks() throws SQLException
    {
	if(null == documentLinks)
	    updateLinksList();

	return documentLinks;
    }

    public Link getSelectedLink()
    {
	return selectedLink;
    }

    public void setSelectedLink(Link selectedLink)
    {
	this.selectedLink = selectedLink;
    }

    public Link getEditLink()
    {
	return editLink;
    }

    public void setEditLink(Link editLink)
    {
	this.editLink = editLink;
    }

    public void removeResourceFromGroup()
    {
	try
	{
	    group.removeResource(clickedResource, getUser());
	    addMessage(FacesMessage.SEVERITY_INFO, "resource_removed_from_group");
	    getUser().setActiveGroup(group);
	    log(Action.group_removing_resource, clickedResource.getId());

	    //loadResources();
	    clickedResource = new Resource();
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	}
    }

    public void deleteResource() throws SQLException
    {
	for(Group group : clickedResource.getGroups())
	{
	    log(Action.group_removing_resource, group.getId(), clickedResource.getId(), "");
	}

	getUser().deleteResource(clickedResource);
	addGrowl(FacesMessage.SEVERITY_INFO, "resource_deleted");
	log(Action.deleting_resource, clickedResource.getId());
	clickedResource = new Resource();
	//loadResources();

    }

    public void removePresentationFromGroup()
    {
	try
	{
	    Connection dbCon = getLearnweb().getConnection();
	    PreparedStatement ps = dbCon.prepareStatement("UPDATE `lw_presentation` SET deleted=1 WHERE `presentation_id`=?");
	    ps.setInt(1, clickedPresentation.getPresentationId());
	    ps.execute();

	    presentations = getLearnweb().getPresentationManager().getPresentationsByGroupId(groupId);
	    clickedPresentation = new Presentation();
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	}
    }

    public void onDeleteLinkFromGroup(int linkId)
    {
	try
	{
	    if(linkId == -1)
	    {
		deleteForum();
	    }
	    else
	    {
		group.deleteLink(linkId);
	    }
	    addMessage(FacesMessage.SEVERITY_INFO, "link_deleted");
	    updateLinksList();
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_INFO, "sorry an error occurred");
	}
    }

    public void onCreateForum()
    {
	if(group.getForumId() != 0) // meanwhile the forum has been created by an other user
	{
	    addMessage(FacesMessage.SEVERITY_INFO, "forum_created");
	    return;
	}

	JForumManager fm = getLearnweb().getJForumManager();

	try
	{
	    int forumId = fm.createForum(group.getTitle(), group.getCourse().getForumCategoryId());

	    group.setForumId(forumId);
	    getLearnweb().getGroupManager().save(group);

	    addMessage(FacesMessage.SEVERITY_INFO, "forum_created");
	    updateLinksList();

	    log(Action.group_adding_link, group.getId(), "Forum");
	}
	catch(Exception e)
	{
	    e.printStackTrace();

	    addMessage(FacesMessage.SEVERITY_ERROR, "The forum couldn't be created, try again later");
	}
    }

    private void deleteForum() throws SQLException
    {
	group.setForumId(0);
	getLearnweb().getGroupManager().save(group);

	updateLinksList();

	log(Action.group_deleting_link, group.getId(), "Forum");
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

    public void onAddLink()
    {
	try
	{
	    if(!group.isMember(getUser()))
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "You are not a member of this group");
		return;
	    }

	    LinkType type;

	    if(!newLinkType.equals("url")) // newLinkType == google document
	    {
		newLinkUrl = new GoogleDriveManager().createEmptyDocument(group.getTitle() + " - " + newLinkTitle, newLinkType).getAlternateLink();
		type = LinkType.DOCUMENT;
		log(Action.group_adding_document, group.getId(), newLinkTitle);
	    }
	    else
	    {
		type = LinkType.LINK;
		log(Action.group_adding_link, group.getId(), newLinkTitle);
	    }

	    group.addLink(newLinkTitle, newLinkUrl, type);

	    addMessage(FacesMessage.SEVERITY_INFO, "link_added");

	    newLinkUrl = null;
	    newLinkTitle = null;
	    updateLinksList();
	}
	catch(Throwable t)
	{
	    addFatalMessage(t);
	}
    }

    public String onEditLink()
    {
	try
	{
	    if(!group.isMember(getUser()))
	    {
		addMessage(FacesMessage.SEVERITY_ERROR, "You are not a member of this group");
	    }
	    else
	    {
		getLearnweb().getLinkManager().save(selectedLink);
		/*
		group.clearLinksCache();
		documentLinks = group.getLinks();
		*/
		updateLinksList();
		addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
	    }
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
	return getTemplateDir() + "/group/overview.xhtml?faces-redirect=true&includeViewParams=true";
    }

    public void onSelect()
    {
	ResourceManager rm = Learnweb.getInstance().getResourceManager();
	Resource temp;
	try
	{
	    temp = rm.getResource(Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id")));
	    setClickedResource(temp);
	}
	catch(NumberFormatException | SQLException e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");

	}
    }

    public void editClickedResource()
    {
	log(Action.edit_resource, clickedResource.getId(), null);
	try
	{
	    clickedResource.save();
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public void addSelectedResource()
    {
	try
	{
	    Resource newResource;

	    if(clickedResource.getId() == -1) // resource is not yet stored at fedora
		newResource = clickedResource;
	    else
		newResource = clickedResource.clone(); // create a copy 

	    Resource res = getUser().addResource(newResource);
	    if(selectedResourceTargetGroupId != 0)
	    {
		Group targetGroup = getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId);
		targetGroup.addResource(res, getUser());
		getUser().setActiveGroup(selectedResourceTargetGroupId);

		addGrowl(FacesMessage.SEVERITY_INFO, "addedResourceToGroup", clickedResource.getTitle(), targetGroup.getTitle());
	    }
	    else
		addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", clickedResource.getTitle());

	    log(Action.adding_resource, selectedResourceTargetGroupId, res.getId(), "");
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

    public void addSelectedResourceLink() throws SQLException
    {
	try
	{
	    User user = getUser();
	    if(null == user)
	    {
		addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
		return;
	    }

	    // add resource to a group if selected
	    if(selectedResourceTargetGroupId != 0)
	    {
		Group targetGroup = getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId);
		targetGroup.addResource(clickedResource, getUser());
		log(Action.adding_resource, selectedResourceTargetGroupId, clickedResource.getId(), "");

		addGrowl(FacesMessage.SEVERITY_INFO, "addedResourceToGroup", clickedResource.getTitle(), targetGroup.getTitle());
	    }
	    else
	    {
		getUser().addResource(clickedResource);
		addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", clickedResource.getTitle());
	    }
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	}
    }

    public void onSelectPresentation()
    {
	PresentationManager pm = Learnweb.getInstance().getPresentationManager();
	Presentation temp;
	try
	{
	    temp = pm.getPresentationsById(Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id")));
	    setClickedPresentation(temp);
	}
	catch(NumberFormatException | SQLException e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
	}
    }

    public boolean hasViewPermission(User user) throws SQLException
    {
	if(null == group)
	    return false;

	return group.getMembers().contains(user);
    }

    public String getRssLink()
    {
	String hash = MD5.hash(groupId + Learnweb.salt1 + getUser().getId() + Learnweb.salt2);
	return UtilBean.getLearnwebBean().getContextUrl() + "/feed/group.jsf?group_id=" + groupId + "&u=" + getUser().getId() + "&h=" + hash;
    }

    public void onSortingChanged(ValueChangeEvent e)
    {
	// TODO implement
	order = Order.TYPE;
    }

    public String getResourceSorting()
    {
	return resourceSorting;
    }

    public void setResourceSorting(String resourceSorting)
    {
	this.resourceSorting = resourceSorting;
    }

    public boolean isNewestResourceHidden()
    {
	return isNewestResourceHidden;
    }

    public int getSelectedResourceTargetGroupId()
    {
	return selectedResourceTargetGroupId;
    }

    public void setSelectedResourceTargetGroupId(int selectedResourceTargetGroupId)
    {
	this.selectedResourceTargetGroupId = selectedResourceTargetGroupId;
    }

    public void onMoveSelectedResource()
    {
	User user = getUser();
	if(null == user)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
	    return;
	}

	Resource resource = selectedResource;

	if(selectedResourceTargetGroupId != 0)
	{
	    try
	    {
		Group targetGroup = getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId);
		getLearnweb().getResourceManager().moveResourceToGroup(selectedResource, targetGroup, group, getUser());

		log(Action.group_removing_resource, selectedResource.getId());

		user.setActiveGroup(selectedResourceTargetGroupId);

		log(Action.adding_resource, resource.getId(), selectedResourceTargetGroupId + "");

		addGrowl(FacesMessage.SEVERITY_INFO, "addedToResources", resource.getTitle());
	    }
	    catch(Exception e)
	    {
		e.printStackTrace();
		addGrowl(FacesMessage.SEVERITY_FATAL, "fatal_error");
	    }
	}
    }

    public String getSubgroupsLabel()
    {
	String label = group.getSubgroupsLabel();
	if(null != label && label.length() > 0)
	    return label;

	return getLocaleMessage("subgroupsLabel");
    }

    public List<Link> getLinks() throws SQLException
    {
	if(null == links)
	    updateLinksList();

	return links;
    }

    public boolean canDeleteResourceCompletely(Object obj)
    {
	User user = getUser();

	if(user.isModerator())
	    return true;

	Resource resource = null;

	if(obj instanceof ResourceDecorator)
	    resource = ((ResourceDecorator) obj).getResource();
	else if(obj instanceof Resource)
	    resource = (Resource) obj;
	else
	    throw new IllegalArgumentException("Method called with an unexpected class type: " + obj.getClass());

	if(user.getId() == resource.getOwnerUserId())
	    return true;

	return false;
    }

    public boolean canDeleteResourceFromGroup(Object resource) throws SQLException
    {
	if(canDeleteResourceCompletely(resource))
	    return true;

	ResourceDecorator decoratedResource = (ResourceDecorator) resource;

	if(getUser().equals(decoratedResource.getAddedToGroupBy()) || getUser().getId() == getGroup().getLeaderUserId())
	    return true;

	return false;
    }

    public List<Presentation> getPresentations() throws SQLException
    {
	presentations = getLearnweb().getPresentationManager().getPresentationsByGroupId(groupId);
	return presentations;
    }

    public void setPresentations(List<Presentation> presentations)
    {
	this.presentations = presentations;
    }

    public Boolean getNewResourceClicked()
    {
	return newResourceClicked;
    }

    public void setNewResourceClicked(Boolean newResourceClicked)
    {
	editResourceClicked = false;
	this.newResourceClicked = newResourceClicked;
    }

    public Boolean getEditResourceClicked()
    {
	return editResourceClicked;
    }

    public void setEditResourceClicked(Boolean editResourceClicked)
    {
	newResourceClicked = false;
	this.editResourceClicked = editResourceClicked;
    }

    public User getClickedUser()
    {
	return clickedUser;
    }

    public void setClickedUser(User clickedUser)
    {
	this.clickedUser = clickedUser;
    }

    public Group getClickedGroup()
    {
	return clickedGroup;
    }

    public void setClickedGroup(Group clickedGroup)
    {
	this.clickedGroup = clickedGroup;
    }

    public boolean isAllLogs()
    {
	return allLogs;
    }

    public void setAllLogs(boolean allLogs)
    {
	this.allLogs = allLogs;
    }

    public boolean isReloadLogs()
    {
	return reloadLogs;
    }

    public void setReloadLogs(boolean reloadLogs)
    {
	this.reloadLogs = reloadLogs;
    }

    public Presentation getClickedPresentation()
    {
	return clickedPresentation;
    }

    public void setClickedPresentation(Presentation clickedPresentation)
    {
	this.clickedPresentation = clickedPresentation;
    }

    public Resource getClickedResource()
    {
	return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
	if(!editResourceClicked || this.clickedResource != clickedResource)
	{
	    editResourceClicked = false;
	    this.clickedResource = clickedResource;
	}
	newResourceClicked = false;
    }

    public AbstractPaginator getPaginator()
    {
	return paginator;
    }

    public String getQuery()
    {
	return query;
    }

    public void setQuery(String query)
    {
	this.query = query;
    }

    public void onQueryChange() throws SQLException
    {
	if(StringUtils.isEmpty(query))
	{
	    paginator = group.getResources(order);
	    return;
	}

	SolrSearch search = new SolrSearch(query, getUser());
	search.setFilterGroups(groupId);
	search.setResultsPerPage(AbstractPaginator.PAGE_SIZE);
	search.setSkipResourcesWithoutThumbnails(false);

	paginator = new SolrSearch.SearchPaginator(search);
    }

    /*
    
    sub group stuff:
    
    
    private Group newGroup;
    public Group getNewGroup()
    {
    return newGroup;
    }

    public void onCreateGroup()
    {
    if(null == getUser())
        return;

    try
    {
        newGroup.setLeader(getUser());
        newGroup.setCourseId(group.getCourseId());
        group.addSubgroup(newGroup);
        getUser().joinGroup(newGroup);
        getLearnweb().getGroupManager().resetCache();//causes a bug in the menu otherwise

    }
    catch(Exception e)
    {
        e.printStackTrace();
        addMessage(FacesMessage.SEVERITY_FATAL, "fatal_error");
        return;
    }

    // log and show notification
    log(Action.group_creating, group.getId());
    addMessage(FacesMessage.SEVERITY_INFO, "groupCreated", newGroup.getTitle());

    // reset new group var
    newGroup = new Group();
    }
    
    
    public void validateGroupTitle(FacesContext context, UIComponent component, Object value) throws ValidatorException, SQLException
    {
    String title = (String) value;

    if(getLearnweb().getGroupManager().getGroupByTitleFilteredByOrganisation(title, getUser().getOrganisationId()) != null)
    {
        throw new ValidatorException(getFacesMessage(FacesMessage.SEVERITY_ERROR, "title_already_taken"));
    }
    }
     */
}
