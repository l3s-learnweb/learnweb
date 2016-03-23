package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.hibernate.validator.constraints.NotEmpty;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.AbstractPaginator;
import de.l3s.learnweb.Comment;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.GoogleDriveManager;
import de.l3s.learnweb.Group;
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
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.learnweb.solrClient.SolrSearch.SearchPaginator;
import de.l3s.util.MD5;

@ManagedBean
@ViewScoped
public class GroupDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = Logger.getLogger(GroupDetailBean.class);
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private int groupId;
    private Group group;
    private String editedGroupDescription;
    private String editedGroupTitle;
    private int editedGroupLeaderId;

    private List<User> members;

    private List<Presentation> presentations;

    private List<LogEntry> logMessages;
    private ArrayList<NewsEntry> newslist;

    public enum RPAction
    {
	none,
	newResource,
	viewResource,
	editResource,
	newFolder,
	editFolder,
	viewFolder
    }

    private RPAction rightPanelAction = RPAction.none;

    private Resource selectedResource;
    public Resource clickedResource;

    private Folder selectedFolder;
    private Folder clickedFolder;
    private String newFolderName;
    private TreeNode selectedNode;

    public Presentation clickedPresentation;

    private User clickedUser;

    private boolean allLogs = false;
    private boolean reloadLogs = false;

    @NotEmpty
    private String newLinkUrl;
    @NotEmpty
    private String newLinkTitle;
    private String newLinkType;

    private Link selectedLink;
    private Link editLink;

    private List<Link> links; // the same as group.getLinks() but with a link to the forum
    private List<Link> documentLinks;
    private String resourceSorting = "title";
    private Order order = Order.TITLE;

    private boolean isNewestResourceHidden = false;

    private TreeNode selectedTargetNode;
    private int selectedResourceTargetGroupId;
    private int selectedResourceTargetFolderId;

    private String query;
    private SearchFilters searchFilters;
    private AbstractPaginator paginator;

    public GroupDetailBean() throws SQLException
    {
	log.debug("construct group detail bean" + getParameter("group_id"));
	loadGroup();

	if(null == group)
	{
	    /*
	    addMessage(FacesMessage.SEVERITY_ERROR, "invalid group id");
	    log.debug("invalid group id");*/
	    return;
	}

	if(getParameterInt("resource_id") != null)
	    setRightPanelAction("viewResource");

	updateLinksList();

	clickedResource = new Resource();
	clickedUser = new User(); // TODO initialize with null
	clickedPresentation = new Presentation();// TODO initialize with null

	searchFilters = new SearchFilters();
	searchFilters.setMode(MODE.group);

	//updateResourcesFromSolr(); //not necessary on most pages
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
		addFatalMessage(e1);
	    }
	}
    }

    private void convert()
    {
	HashSet<Integer> deletedResources = new HashSet<Integer>();
	Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
		Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource,
		Action.group_removing_resource };
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

    private void loadGroup() throws SQLException
    {
	if(null == group)
	{
	    Integer id = getParameterInt("group_id");

	    if(null == id)
		return;

	    groupId = id.intValue();
	}

	group = getLearnweb().getGroupManager().getGroupById(groupId);
	if(group != null)
	{
	    editedGroupDescription = group.getDescription();
	    editedGroupLeaderId = group.getLeader().getId();
	    editedGroupTitle = group.getTitle();

	    if(null == selectedFolder)
	    {
		Integer id = getParameterInt("folder_id");

		if(null == id)
		{
		    selectedFolder = new Folder(0, groupId, group.getTitle());
		}
		else
		{
		    selectedFolder = getLearnweb().getGroupManager().getFolder(id.intValue());
		    clickedFolder = selectedFolder;
		}
	    }
	}
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
	//loadGroup();
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

    public int getSelectedFolderId()
    {
	return selectedFolder == null || selectedFolder.getFolderId() <= 0 ? 0 : selectedFolder.getFolderId();
    }

    public void setSelectedFolderId(int folderId) throws SQLException
    {
	if(folderId > 0)
	{
	    selectedFolder = getLearnweb().getGroupManager().getFolder(folderId);
	}
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
	    Resource res = clickedResource;
	    res.setGroupId(0);
	    res.save();

	    addMessage(FacesMessage.SEVERITY_INFO, "resource_removed_from_group");
	    getUser().setActiveGroup(group);
	    log(Action.group_removing_resource, clickedResource.getId());

	    //loadResources();
	    clickedResource = new Resource();
	    updateResourcesFromSolr();
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	}
    }

    public void deleteResource() throws SQLException
    {
	log(Action.group_removing_resource, clickedResource.getGroupId(), clickedResource.getId(), "");

	//getUser().deleteResource(clickedResource);

	clickedResource.setGroupId(0);
	clickedResource.setFolderId(0);
	clickedResource.save();

	addGrowl(FacesMessage.SEVERITY_INFO, "resource_deleted");
	log(Action.deleting_resource, clickedResource.getId());

	//loadResources();
	clickedResource = new Resource();
	updateResourcesFromSolr();
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

	    group.deleteLink(linkId);

	    addMessage(FacesMessage.SEVERITY_INFO, "link_deleted");
	    updateLinksList();
	}
	catch(SQLException e)
	{
	    e.printStackTrace();
	    addMessage(FacesMessage.SEVERITY_INFO, "sorry an error occurred");
	}
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
	ResourceManager rm = getLearnweb().getResourceManager();
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

	    Group targetGroup = getLearnweb().getGroupManager().getGroupById(selectedResourceTargetGroupId);

	    newResource.setGroupId(selectedResourceTargetGroupId);
	    newResource.setFolderId(selectedResourceTargetFolderId);
	    Resource res = getUser().addResource(newResource);

	    if(selectedResourceTargetGroupId != 0)
	    {
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

    public void onSelectPresentation()
    {
	PresentationManager pm = getLearnweb().getPresentationManager();
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

    public int getSelectedResourceTargetFolderId()
    {
	return selectedResourceTargetFolderId;
    }

    public void setSelectedResourceTargetFolderId(int selectedResourceTargetFolderId)
    {
	this.selectedResourceTargetFolderId = selectedResourceTargetFolderId;
    }

    public TreeNode getSelectedTargetNode()
    {
	return selectedTargetNode;
    }

    public void setSelectedTargetNode(TreeNode selectedTargetNode)
    {
	this.selectedTargetNode = selectedTargetNode;
    }

    public void onTargetNodeSelect(NodeSelectEvent event)
    {
	String type = event.getTreeNode().getType();

	if(type.equals("group"))
	{
	    Group group = (Group) event.getTreeNode().getData();
	    if(group != null)
	    {
		selectedResourceTargetGroupId = group.getId();
		selectedResourceTargetFolderId = 0;
	    }
	}
	else if(type.equals("folder"))
	{
	    Folder folder = (Folder) event.getTreeNode().getData();
	    if(folder != null)
	    {
		selectedResourceTargetGroupId = folder.getGroupId();
		selectedResourceTargetFolderId = folder.getFolderId();
	    }
	}
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
		resource.setGroup(targetGroup);
		resource.save();

		user.setActiveGroup(selectedResourceTargetGroupId);

		log(Action.group_removing_resource, selectedResource.getId());
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

    public boolean canMoveResourcesInGroup() throws SQLException
    {
	if((!getGroup().isReadOnly() && (!getGroup().isRestrictionOnlyLeaderCanAddResources() || getUser().getId() == getGroup().getLeaderUserId())) || getUser().isModerator())
	    return true;

	return false;
    }

    public boolean canEditFoldersInGroup() throws SQLException
    {
	if((!getGroup().isReadOnly() && (!getGroup().isRestrictionOnlyLeaderCanAddResources() || getUser().getId() == getGroup().getLeaderUserId())) || getUser().isModerator())
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

    public RPAction getRightPanelAction()
    {
	return rightPanelAction;
    }

    public void setRightPanelAction(RPAction rightPanelAction)
    {
	this.rightPanelAction = rightPanelAction;
    }

    public void setRightPanelAction(String value)
    {
	try
	{
	    this.rightPanelAction = RPAction.valueOf(value);
	}
	catch(Exception e)
	{
	    this.rightPanelAction = null;
	}
    }

    public User getClickedUser()
    {
	return clickedUser;
    }

    public void setClickedUser(User clickedUser)
    {
	this.clickedUser = clickedUser;
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
	if(this.rightPanelAction != RPAction.editResource || this.clickedResource != clickedResource)
	{
	    this.clickedResource = clickedResource;
	    this.rightPanelAction = RPAction.viewResource;
	}
    }

    public AbstractPaginator getPaginator()
    {
	if(null == paginator)
	    updateResourcesFromSolr();

	return paginator;
    }

    public String changeFilters(String queryFilters)
    {
	searchFilters.setFiltersFromString(queryFilters);
	updateResourcesFromSolr();
	return queryFilters;
    }

    public void onQueryFiltersChange() throws SQLException
    {
	updateResourcesFromSolr();
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
	updateResourcesFromSolr();
	log(Action.group_resource_search, groupId, 0, query);
    }

    public void saveGmailId()
    {
	String gmailId = getParameter("gmail_id");
	try
	{
	    Learnweb.getInstance().getUserManager().saveGmailId(gmailId, getUser().getId());
	}
	catch(SQLException e)
	{
	    log.error("Error while inserting gmail id" + e);
	}
    }

    public List<Filter> getAvailableFilters()
    {
	return searchFilters.getAvailableFilters();
    }

    public String getSearchFilters()
    {
	return searchFilters.getFiltersString();
    }

    public void updateResourcesFromSolr()
    {
	if(this.searchFilters == null)
	{
	    return;
	}

	int folderId = (selectedFolder != null && selectedFolder.getFolderId() > 0) ? selectedFolder.getFolderId() : 0;
	try
	{
	    paginator = getResourcesFromSolr(groupId, folderId, query, getUser());
	}
	catch(SQLException | SolrServerException e)
	{
	    addFatalMessage(e);
	}
    }

    private SearchPaginator getResourcesFromSolr(int groupId, int folderId, String query, User user) throws SQLException, SolrServerException
    {
	SolrSearch solrSearch = new SolrSearch(StringUtils.isEmpty(query) ? "*" : query, user);
	solrSearch.setFilterGroups(groupId);
	solrSearch.setFilterFolder(folderId, !StringUtils.isEmpty(query));
	solrSearch.setResultsPerPage(AbstractPaginator.PAGE_SIZE);
	solrSearch.setSkipResourcesWithoutThumbnails(false);
	solrSearch.setFacetFields(searchFilters.getFacetFields());
	solrSearch.setFacetQueries(searchFilters.getFacetQueries());
	solrSearch.setSort("timestamp DESC");

	if(searchFilters.getServiceFilter() != null)
	    solrSearch.setFilterLocation(searchFilters.getServiceFilter());
	if(searchFilters.getTypeFilter() != null)
	    solrSearch.setFilterType(searchFilters.getTypeFilter());
	if(searchFilters.getDateFromFilterAsString() != null)
	    solrSearch.setFilterDateFrom(SOLR_DATE_FORMAT.format(searchFilters.getDateFromFilter()));
	if(searchFilters.getDateToFilterAsString() != null)
	    solrSearch.setFilterDateTo(SOLR_DATE_FORMAT.format(searchFilters.getDateToFilter()));
	if(searchFilters.getCollectorFilter() != null)
	    solrSearch.setFilterCollector(searchFilters.getCollectorFilter());
	if(searchFilters.getAuthorFilter() != null)
	    solrSearch.setFilterAuthor(searchFilters.getAuthorFilter());
	if(searchFilters.getCoverageFilter() != null)
	    solrSearch.setFilterCoverage(searchFilters.getCoverageFilter());
	if(searchFilters.getPublisherFilter() != null)
	    solrSearch.setFilterPublisher(searchFilters.getPublisherFilter());
	if(searchFilters.getTagsFilter() != null)
	    solrSearch.setFilterTags(searchFilters.getTagsFilter());

	SearchPaginator sp = new SolrSearch.SearchPaginator(solrSearch);
	searchFilters.putResourceCounter(sp.getFacetFields());
	searchFilters.putResourceCounter(sp.getFacetQueries());
	return sp;
    }

    public List<Folder> getSubfolders() throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getFolders(groupId, getSelectedFolderId());
    }

    public String getCurrentPath() throws SQLException
    {
	if(this.group != null)
	{
	    return this.selectedFolder == null ? this.group.getTitle() : selectedFolder.getPrettyPath();
	}

	return null;
    }

    public TreeNode getFoldersTree(int groupId) throws SQLException
    {
	return Learnweb.getInstance().getGroupManager().getFoldersTree(groupId, getSelectedFolderId());
    }

    public void moveToFolder()
    {
	Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
	String strDestFolderId = params.get("destFolderId");
	String type = params.get("type");
	String objectId = params.get("objectId");

	try
	{
	    int destFolderId = Integer.parseInt(strDestFolderId);

	    if(type.equals("folder"))
	    {
		Folder folder = Learnweb.getInstance().getGroupManager().getFolder(Integer.parseInt(objectId));
		folder.moveTo(groupId, destFolderId);
	    }
	    else if(type.equals("resource"))
	    {
		Resource res = Learnweb.getInstance().getResourceManager().getResource(Integer.parseInt(objectId));
		res.moveTo(groupId, destFolderId);
		updateResourcesFromSolr();
	    }
	    else
	    {
		throw new Exception("wrong type");
	    }
	}
	catch(Exception e)
	{

	}
    }

    public void addFolder() throws SQLException
    {
	if(canEditFoldersInGroup() && newFolderName != null && !newFolderName.isEmpty() && selectedFolder != null)
	{
	    Folder newFolder = new Folder(groupId, newFolderName);
	    newFolder.setParentFolderId(getSelectedFolderId());
	    newFolder.setUser(getUser());
	    newFolder.save();

	    addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", newFolder.getName());
	}

	newFolderName = null;
    }

    public void editFolder() throws SQLException
    {
	if(canEditFoldersInGroup() && clickedFolder != null && clickedFolder.getFolderId() > 0)
	{
	    log(Action.edit_folder, clickedFolder.getFolderId(), null);

	    try
	    {
		clickedFolder.save();
		addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", clickedFolder.getName());
	    }
	    catch(SQLException e)
	    {
		addFatalMessage(e);
	    }
	}
    }

    public void deleteFolder() throws SQLException
    {
	if(canEditFoldersInGroup() && clickedFolder != null)
	{
	    String folderName = clickedFolder.getName();

	    if(selectedFolder == clickedFolder)
	    {
		selectedFolder = selectedFolder.getParentFolder() == null ? null : selectedFolder.getParentFolder();
	    }

	    clickedFolder.delete();
	    clickedFolder = null;

	    addMessage(FacesMessage.SEVERITY_INFO, "folderDeleted", folderName);
	}
    }

    public Folder getSelectedFolder()
    {
	return selectedFolder;
    }

    public void setSelectedFolder(Folder folder)
    {
	if(folder != null)
	{
	    this.selectedFolder = folder;
	    this.clickedFolder = selectedFolder;

	    updateResourcesFromSolr();
	    UtilBean.getAddResourceBean().setResourceTargetFolderId(getSelectedFolderId());
	}
    }

    public Folder getClickedFolder()
    {
	return clickedFolder;
    }

    public void setClickedFolder(Folder clickedFolder)
    {
	if(this.clickedFolder != null && this.clickedFolder.equals(clickedFolder))
	{
	    this.rightPanelAction = RPAction.none;
	    setSelectedFolder(this.clickedFolder);
	}
	else
	{
	    this.rightPanelAction = RPAction.viewFolder;
	    this.clickedFolder = clickedFolder;
	}
    }

    public TreeNode getSelectedNode()
    {
	return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode)
    {
	this.selectedNode = selectedNode;
    }

    public void onNodeSelect(NodeSelectEvent event)
    {
	Folder selectedFolder = (Folder) selectedNode.getData();
	setSelectedFolder(selectedFolder);
    }

    public String getNewFolderName()
    {
	return newFolderName;
    }

    public void setNewFolderName(String newFolderName)
    {
	this.newFolderName = newFolderName;
    }

    public void setGroup(Group group)
    {
	this.group = group;
    }

    public List<SelectItem> getMembersSelectItemList() throws SQLException
    {
	if(null == group)
	    return new ArrayList<SelectItem>();

	List<SelectItem> yourList;
	yourList = new ArrayList<SelectItem>();

	for(User member : group.getMembers())
	    yourList.add(new SelectItem(member.getId(), member.getUsername()));

	return yourList;
    }

    public String getEditedGroupDescription()
    {
	return editedGroupDescription;
    }

    public void setEditedGroupDescription(String editedGroupDescription)
    {
	this.editedGroupDescription = editedGroupDescription;
    }

    public String getEditedGroupTitle()
    {
	return editedGroupTitle;
    }

    public void setEditedGroupTitle(String editedGroupTitle)
    {
	this.editedGroupTitle = editedGroupTitle;
    }

    public int getEditedGroupLeaderId()
    {
	return editedGroupLeaderId;
    }

    public void setEditedGroupLeaderId(int editedGroupLeaderId)
    {
	this.editedGroupLeaderId = editedGroupLeaderId;
    }

    public void onMoveResource()
    {

	//clickedResource.setTitle("sldfjsdif");
	//clickedResource.save();

	//getLearnweb().getSolrClient().reIndexResource(resource);
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
		log(Action.group_changing_description, group.getId());
	    }
	    if(!editedGroupTitle.equals(group.getTitle()))
	    {
		log(Action.group_changing_title, group.getId(), group.getTitle());
		group.setTitle(editedGroupTitle);
	    }
	    if(editedGroupLeaderId != group.getLeaderUserId())
	    {
		group.setLeaderUserId(editedGroupLeaderId);
		log(Action.group_changing_leader, group.getId());
	    }
	    getLearnweb().getGroupManager().save(group);
	    //getLearnweb().getGroupManager().resetCache();
	    getUser().clearCaches();

	}
	catch(SQLException e)
	{
	    addGrowl(FacesMessage.SEVERITY_ERROR, "fatal error");
	    e.printStackTrace();
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
	    e.printStackTrace();
	}
	addGrowl(FacesMessage.SEVERITY_INFO, "Copied Resources");
    }

    public List<Group> getUserCopyableGroups() throws SQLException
    {
	List<Group> copyableGroups = getUser().getWriteAbleGroups();
	copyableGroups.remove(group);
	return copyableGroups;
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
