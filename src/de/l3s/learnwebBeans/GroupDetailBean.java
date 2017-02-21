package de.l3s.learnwebBeans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.hibernate.validator.constraints.NotEmpty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.context.RequestContext;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.AbstractPaginator;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.GoogleDriveManager;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupItem;
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
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.learnweb.solrClient.SolrSearch.SearchPaginator;
import de.l3s.util.StringHelper;

@ManagedBean(name = "groupDetailBean")
@ViewScoped
public class GroupDetailBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = Logger.getLogger(GroupDetailBean.class);
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Group base attributes
    private int groupId; // Current group id
    private Group group; // Current group
    private Folder selectedFolder; // Current opened folder
    private TreeNode selectedNode; // Current folder in left panel
    private List<Folder> breadcrumbs;

    private List<User> members;
    private List<Presentation> presentations;
    private List<LogEntry> logMessages;
    private ArrayList<NewsEntry> newslist;

    // Group edit fields (Required for editing group)
    private String editedGroupDescription;
    @NotEmpty
    @Size(min = 3, max = 60)
    private String editedGroupTitle;
    private int editedGroupLeaderId;

    private User clickedUser;
    private Presentation clickedPresentation;
    private GroupItem clickedGroupItem; // Preview of resource/folder
    private RPAction rightPanelAction = RPAction.none;

    private boolean allLogs = false;
    private boolean reloadLogs = false;
    private boolean isNewestResourceHidden = false;

    // New folder form
    private String newFolderName;
    private String newFolderDescription;

    // New link form
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

    // Folders tree
    private TreeNode selectedTargetNode;
    private int selectedResourceTargetGroupId;
    private int selectedResourceTargetFolderId;

    // In group search/filters
    private String query;
    private SearchFilters searchFilters;
    private AbstractPaginator paginator;

    @ManagedProperty(value = "#{resourceDetailBean}")
    private ResourceDetailBean resourceDetailBean;

    @ManagedProperty(value = "#{addResourceBean}")
    private AddResourceBean addResourceBean;

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

    public GroupDetailBean() throws SQLException
    {
        loadGroup();

        if(null == group)
        {
            return;
        }

        if(getParameterInt("resource_id") != null)
            setRightPanelAction("viewResource");

        updateLinksList();

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

    /**
     * die funktion ist totale scheisse. ersetzen sobald es geht
     *
     * @throws SQLException
     */
    private void convert() throws SQLException
    {
        /*
        HashSet<Integer> deletedResources = new HashSet<Integer>();
        	Action[] filter = new Action[] { Action.adding_resource, Action.commenting_resource, Action.edit_resource, Action.deleting_resource, Action.group_adding_document, Action.group_adding_link, Action.group_changing_description, Action.group_changing_leader,
        	Action.group_changing_restriction, Action.group_changing_title, Action.group_creating, Action.group_deleting, Action.group_joining, Action.group_leaving, Action.rating_resource, Action.tagging_resource, Action.thumb_rating_resource,
        	Action.group_removing_resource };
        	*/
        List<LogEntry> feed = logMessages;

        if(feed != null)
        {
            ResourceManager resourceManager = getLearnweb().getResourceManager();

            newslist = new ArrayList<NewsEntry>();
            for(LogEntry l : feed)
            {
                newslist.add(new NewsEntry(l));

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
                    buildBreadcrumbsForFolder(selectedFolder);
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

            convert();
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
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
        return selectedFolder == null || selectedFolder.getId() <= 0 ? 0 : selectedFolder.getId();
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
                log(Action.group_adding_document, group.getId(), group.getId(), newLinkTitle);
            }
            else
            {
                type = LinkType.LINK;
                log(Action.group_adding_link, group.getId(), group.getId(), newLinkTitle);
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
            addFatalMessage(e);
        }
        return getTemplateDir() + "/group/overview.xhtml?faces-redirect=true&includeViewParams=true";
    }

    public void editClickedResource()
    {
        log(Action.edit_resource, clickedGroupItem.getGroupId(), clickedGroupItem.getId(), null);
        try
        {
            clickedGroupItem.save();
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
            addFatalMessage(e);
        }
    }

    public boolean hasViewPermission(User user) throws SQLException
    {
        if(null == group)
            return false;

        return group.getMembers().contains(user);
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
                selectedResourceTargetFolderId = folder.getId();
            }
        }
    }

    public void updateTargetForAddResourceBean()
    {
        this.getAddResourceBean().setResourceTargetGroupId(selectedResourceTargetGroupId);
        this.getAddResourceBean().setResourceTargetFolderId(selectedResourceTargetFolderId);
    }

    public List<Link> getLinks() throws SQLException
    {
        if(null == links)
            updateLinksList();

        return links;
    }

    public boolean canDeleteTheResourceCompletely(Object obj) throws SQLException
    {
        User user = getUser();

        if(user == null)
            return false;

        if(user.isAdmin() || group.getCourse().isModerator(user))
        {
            return true;
        }

        GroupItem resource;
        if(obj instanceof ResourceDecorator)
            resource = ((ResourceDecorator) obj).getResource();
        else if(obj instanceof Resource)
            resource = (Resource) obj;
        else if(obj instanceof Folder)
            resource = (Folder) obj;
        else
            throw new IllegalArgumentException("Method called with an unexpected class type: " + obj.getClass());

        return user.getId() == resource.getUserId();
    }

    public boolean canEditTheResource(GroupItem obj) throws SQLException
    {
        if(obj.getGroupId() == 0 && obj.getUserId() == getUser().getId())
            return true;

        return canEditResourcesInGroup(obj.getGroup());
    }

    public boolean canDeleteTheResource(GroupItem obj) throws SQLException
    {
        return canDeleteResourcesInGroup(obj.getGroup());
    }

    public boolean canDeleteResourcesInGroup(Group group) throws SQLException
    {
        return group.canDeleteResources(getUser());
    }

    public boolean canDeleteResourcesInTheGroup() throws SQLException
    {
        return canDeleteResourcesInGroup(getGroup());
    }

    public boolean canEditResourcesInGroup(Group group) throws SQLException
    {
        return group.canEditResources(getUser());
    }

    public boolean canEditResourcesInTheGroup() throws SQLException
    {
        return canEditResourcesInGroup(getGroup());
    }

    public boolean canCopyResourcesFromTheGroup() throws SQLException
    {
        return canSeeResourcesInTheGroup();
    }

    public boolean canSeeResourcesInTheGroup() throws SQLException
    {
        return getGroup().canViewResources(getUser());
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
            getLearnweb().getUserManager().saveGmailId(gmailId, getUser().getId());
        }
        catch(SQLException e)
        {
            log.error("Error while inserting gmail id" + e);
        }
    }

    public List<Filter> getAvailableFilters()
    {
        getPaginator();
        if(searchFilters == null) // should only happen for private resources
            return null;

        return searchFilters.getAvailableFilters();
    }

    public String getSearchFilters()
    {
        return searchFilters != null ? searchFilters.getFiltersString() : null;
    }

    public void updateResourcesFromSolr()
    {
        if(this.searchFilters == null)
        {
            return;
        }

        int folderId = (selectedFolder != null && selectedFolder.getId() > 0) ? selectedFolder.getId() : 0;
        try
        {
            paginator = getResourcesFromSolr(groupId, folderId, query, getUser());
            //TODO: remove it
            RequestContext.getCurrentInstance().update(":filters");
        }
        catch(SQLException | IOException | SolrServerException e)
        {
            addFatalMessage(e);
        }
    }

    private SearchPaginator getResourcesFromSolr(int groupId, int folderId, String query, User user) throws SQLException, IOException, SolrServerException
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
        return getLearnweb().getGroupManager().getFolders(groupId, getSelectedFolderId());
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
        return getLearnweb().getGroupManager().getFoldersTree(groupId, getSelectedFolderId());
    }

    /**
     * Action used to create new folder
     */
    public void addFolder() throws SQLException
    {
        if(canEditResourcesInTheGroup() && newFolderName != null && !newFolderName.isEmpty())
        {
            Folder newFolder = new Folder(groupId, newFolderName, newFolderDescription);
            newFolder.setParentFolderId(getSelectedFolderId());
            newFolder.setUser(getUser());
            newFolder.save();

            log(Action.add_folder, newFolder.getGroupId(), newFolder.getId(), newFolder.getTitle());

            addMessage(FacesMessage.SEVERITY_INFO, "folderCreated", newFolder.getTitle());
        }

        newFolderName = null;
        newFolderDescription = null;
    }

    /**
     * Action used to create edit folder
     */
    public void editFolder() throws SQLException
    {
        if(canEditResourcesInTheGroup() && clickedGroupItem != null && clickedGroupItem.getId() > 0)
        {
            log(Action.edit_folder, groupId, clickedGroupItem.getId(), clickedGroupItem.getTitle());

            try
            {
                clickedGroupItem.save();
                addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", clickedGroupItem.getTitle());
            }
            catch(SQLException e)
            {
                addFatalMessage(e);
            }
        }
    }

    public GroupItem getClickedGroupItem()
    {
        return clickedGroupItem;
    }

    public void setClickedGroupItem(GroupItem clickedGroupItem)
    {
        this.clickedGroupItem = clickedGroupItem;
        if(clickedGroupItem instanceof Resource)
        {
            this.getResourceDetailBean().setClickedResource((Resource) clickedGroupItem);
            this.rightPanelAction = RPAction.viewResource;
        }
        else
        {
            this.rightPanelAction = RPAction.viewFolder;
        }
    }

    @Deprecated
    public Resource getClickedResource()
    {
        if(clickedGroupItem instanceof Resource)
        {
            return (Resource) getClickedGroupItem();
        }

        return null;
    }

    @Deprecated
    public void setClickedResource(Resource clickedResource)
    {
        setClickedGroupItem(clickedResource);
    }

    @Deprecated
    public Folder getClickedFolder()
    {
        if(clickedGroupItem instanceof Folder)
        {
            return (Folder) getClickedGroupItem();
        }

        return null;
    }

    @Deprecated
    public void setClickedFolder(Folder clickedFolder)
    {
        setClickedGroupItem(clickedFolder);
    }

    public Folder getSelectedFolder()
    {
        return selectedFolder;
    }

    public void setSelectedFolder(Folder folder)
    {
        this.selectedFolder = folder;
        updateResourcesFromSolr();
        buildBreadcrumbsForFolder(folder);

        this.clickedGroupItem = null;
        this.rightPanelAction = RPAction.none;
        this.getAddResourceBean().setResourceTargetFolderId(getSelectedFolderId());
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

    public String getNewFolderDescription() {
        return newFolderDescription;
    }

    public void setNewFolderDescription(String newFolderDescription) {
        this.newFolderDescription = newFolderDescription;
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

    public static long getSerialVersionUID()
    {
        return serialVersionUID;
    }

    public static Logger getLog()
    {
        return log;
    }

    public static DateFormat getSolrDateFormat()
    {
        return SOLR_DATE_FORMAT;
    }

    public void setMembers(List<User> members)
    {
        this.members = members;
    }

    public void setLogMessages(List<LogEntry> logMessages)
    {
        this.logMessages = logMessages;
    }

    public void setNewslist(ArrayList<NewsEntry> newslist)
    {
        this.newslist = newslist;
    }

    public void setNewestResourceHidden(boolean newestResourceHidden)
    {
        isNewestResourceHidden = newestResourceHidden;
    }

    public void setLinks(List<Link> links)
    {
        this.links = links;
    }

    public void setDocumentLinks(List<Link> documentLinks)
    {
        this.documentLinks = documentLinks;
    }

    public Order getOrder()
    {
        return order;
    }

    public void setOrder(Order order)
    {
        this.order = order;
    }

    public void setSearchFilters(SearchFilters searchFilters)
    {
        this.searchFilters = searchFilters;
    }

    public void setPaginator(AbstractPaginator paginator)
    {
        this.paginator = paginator;
    }

    public List<Folder> getBreadcrumbs()
    {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<Folder> breadcrumbs)
    {
        this.breadcrumbs = breadcrumbs;
    }

    private void buildBreadcrumbsForFolder(Folder folder)
    {
        breadcrumbs = new ArrayList<>();

        try
        {
            addFolderToBreadcrumbs(folder);
        }
        catch(SQLException e)
        {
            log.warn("Can not get parent folder.");
        }
    }

    private void addFolderToBreadcrumbs(Folder folder) throws SQLException
    {
        if(folder != null && folder.getId() != 0)
        {
            breadcrumbs.add(0, folder);
            addFolderToBreadcrumbs(folder.getParentFolder());
        }
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

    public ResourceDetailBean getResourceDetailBean()
    {
        return resourceDetailBean;
    }

    public void setResourceDetailBean(ResourceDetailBean resourceDetailBean)
    {
        this.resourceDetailBean = resourceDetailBean;
    }

    public AddResourceBean getAddResourceBean()
    {
        return addResourceBean;
    }

    public void setAddResourceBean(AddResourceBean addResourceBean)
    {
        this.addResourceBean = addResourceBean;
    }

    public void actionOpenFolder()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            int folderId = StringHelper.parseInt(params.get("itemId"));
            if(folderId > 0)
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(folderId);
                if(folder != null)
                    this.setSelectedFolder(folder);
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else
            {
                this.setSelectedFolder(null);
            }
        }
        catch(NullPointerException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void actionSelectGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String itemType = params.get("itemType");
            int itemId = StringHelper.parseInt(params.get("itemId"), -1);

            if(itemType != null && itemType.equals("folder") && itemId > 0)
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
                if(folder != null)
                    this.setClickedGroupItem(folder);
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                    this.setClickedGroupItem(resource);
                else
                    throw new NullPointerException("Target resource does not exists");
            }
            else
            {
                throw new NullPointerException("Unsupported element type");
            }
        }
        catch(NullPointerException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void actionEditGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        try
        {
            String itemType = params.get("itemType");
            int itemId = StringHelper.parseInt(params.get("itemId"), -1);

            if(itemType != null && itemType.equals("folder") && itemId > 0)
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
                if(folder != null && canEditTheResource(folder))
                {
                    this.setClickedGroupItem(folder);
                    this.setRightPanelAction(RPAction.editFolder);
                }
                else
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "Target folder doesn't exists or you don't have permission to edit it");
                }
            }
            else if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null && canEditTheResource(resource))
                {
                    this.setClickedGroupItem(resource);
                    this.setRightPanelAction(RPAction.editResource);
                }
                else
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "Target resource doesn't exists or you don't have permission to edit it");
                }
            }
            else
            {
                throw new NullPointerException("Unsupported itemType");

            }
        }
        catch(NullPointerException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void actionCreateGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String type = params.get("type");

        switch(type)
        {
        case "folder":
            this.setRightPanelAction(RPAction.newFolder);
            break;
        case "file":
            this.setRightPanelAction(RPAction.newResource);
            this.getAddResourceBean().clearForm();
            this.getAddResourceBean().getResource().setStorageType(1);
            break;
        case "url":
            this.setRightPanelAction(RPAction.newResource);
            this.getAddResourceBean().clearForm();
            this.getAddResourceBean().getResource().setStorageType(2);
            break;
        case "glossary":
            this.setRightPanelAction(RPAction.newResource);
            this.getAddResourceBean().clearForm();
            this.getAddResourceBean().getResource().setStorageType(3);
            break;
        default:
            log.warn("Unsupported item type: " + type);
            break;
        }

        this.getAddResourceBean().setResourceTargetGroupId(this.groupId);
        this.getAddResourceBean().setResourceTargetFolderId(this.getSelectedFolderId());
    }

    public void actionUpdateGroupItems()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String action = params.get("action");

        try
        {
            JSONArray items = new JSONArray(params.get("items"));

            switch(action)
            {
            case "copy":
                this.actionCopyGroupItems(items);
                break;
            case "move":
                JSONObject dest = params.containsKey("destination") ? new JSONObject(params.get("destination")) : null;
                this.moveGroupItems(items, dest);
                break;
            case "delete":
                this.deleteGroupItems(items);
                break;
            case "add-tag":
                String tag = params.get("tag");
                this.addTagToGroupItems(items, tag);
                break;
            default:
                log.warn("Unsupported action: " + action);
                break;
            }
        }
        catch(JSONException e)
        {
            addFatalMessage(e);
        }
    }

    private void actionCopyGroupItems(JSONArray objects)
    {
        try
        {
            int numFolders = 0, numResources = 0, numSkipped = 0, targetGroupId = selectedResourceTargetGroupId, targetFolderId = selectedResourceTargetFolderId;

            Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
            if(!canCopyResourcesFromTheGroup())
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to copy this resource");
                return;
            }

            if(targetGroupId != 0 && !canEditResourcesInGroup(targetGroup))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to add new resources in target group");
                return;
            }

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);
                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"), -1);
                if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null)
                    {
                        Resource newResource = resource.clone();
                        newResource.setGroupId(targetGroupId);
                        newResource.setFolderId(targetFolderId);
                        resource = getUser().addResource(newResource);
                        numResources++;
                        log(Action.adding_resource, targetGroupId, resource.getId(), "");
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target resource does not exists on actionCopyGroupItems");
                    }
                }
                else
                {
                    numSkipped++;
                    log.warn("Unsupported itemType");
                }
            }

            if(numFolders + numResources > 0)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "resourcesCopiedSuccessfully", numFolders + numResources);
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "resourcesCanNotBeChanged", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    private void moveGroupItems(JSONArray objects, JSONObject dest)
    {
        try
        {
            int numFolders = 0, numResources = 0, numSkipped = 0, targetGroupId = selectedResourceTargetGroupId, targetFolderId = selectedResourceTargetFolderId;

            if(dest != null)
            {
                targetGroupId = StringHelper.parseInt(dest.getString("groupId"), groupId);
                targetFolderId = StringHelper.parseInt(dest.getString("folderId"), 0);
            }

            Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
            if(!canEditResourcesInTheGroup())
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to move this resource");
                return;
            }

            if(targetGroupId != 0 && !canEditResourcesInGroup(targetGroup))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to add new resources in target group");
                return;
            }

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);
                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"));
                if(itemType != null && itemType.equals("folder") && itemId > 0)
                {
                    Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
                    if(folder != null)
                    {
                        if(!canDeleteTheResource(folder))
                        {
                            numSkipped++;
                            log.warn("The use don't have permissions to delete folder in target group.");
                            continue;
                        }

                        folder.moveTo(targetGroupId, targetFolderId);
                        numFolders++;
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target folder does not exists on actionMoveGroupItems");
                    }
                }
                else if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null)
                    {
                        if(!canDeleteTheResource(resource))
                        {
                            numSkipped++;
                            log.warn("The use don't have permissions to delete resource in target group.");
                            continue;
                        }

                        resource.moveTo(targetGroupId, targetFolderId);
                        numResources++;
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target folder does not exists on actionMoveGroupItems");
                    }
                }
                else
                {
                    numSkipped++;
                    log.warn("Unsupported itemType");
                }
            }

            if(numFolders + numResources > 0)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "resourcesMovedSuccessfully", numFolders + numResources);
                if(numResources > 0)
                    this.updateResourcesFromSolr();
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "resourcesCanNotBeChanged", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    private void deleteGroupItems(JSONArray objects)
    {
        try
        {
            int numFolders = 0, numResources = 0, numSkipped = 0;

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);

                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"));

                if(itemType != null && itemType.equals("folder") && itemId > 0)
                {
                    Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
                    if(folder != null)
                    {
                        if(!canDeleteTheResource(folder))
                        {
                            numSkipped++;
                            log.warn("The use don't have permissions to delete folder in target group.");
                            continue;
                        }

                        int folderGroupId = folder.getGroupId();
                        String folderName = folder.getTitle();
                        if(clickedGroupItem != null && clickedGroupItem.equals(folder))
                            clickedGroupItem = null;

                        if(selectedFolder != null && selectedFolder.equals(folder))
                            selectedFolder = null;

                        folder.delete();
                        numFolders++;

                        log(Action.deleting_folder, folderGroupId, itemId, folderName);
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target folder does not exists on actionDeleteGroupItems");
                    }
                }
                else if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null)
                    {
                        if(!resource.canDeleteResource(getUser()))
                        {
                            numSkipped++;
                            log.warn("The use don't have permissions to delete resource in target group.");
                            continue;
                        }

                        int resourceGroupId = resource.getGroupId();
                        String resourceTitle = resource.getTitle();
                        if(clickedGroupItem != null && clickedGroupItem.equals(resource))
                            clickedGroupItem = null;

                        resource.delete();
                        numResources++;

                        log(Action.deleting_resource, resourceGroupId, itemId, resourceTitle);
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target resource does not exists on actionDeleteGroupItems");
                    }
                }
                else
                {
                    numSkipped++;
                    log.warn("Unsupported itemType");
                }
            }

            if(numFolders + numResources > 0)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "resourcesDeletedSuccessfully", numFolders + numResources);
                if(numResources > 0)
                    this.updateResourcesFromSolr();
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "resourcesCanNotBeChanged", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addFatalMessage(e);
        }
    }

    private void addTagToGroupItems(JSONArray objects, String tag)
    {
        try
        {
            int numResources = 0, numSkipped = 0;
            if(!canEditResourcesInTheGroup())
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to edit this resource");
                return;
            }

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);

                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"), -1);

                if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null)
                    {
                        resource.addTag(tag, getUser());
                        numResources++;
                        log(Action.tagging_resource, resource.getGroupId(), resource.getId(), tag);
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Target resource does not exists on actionAddTagToGroupItems");
                    }
                }
                else
                {
                    numSkipped++;
                    log.warn("Unsupported itemType");
                }
            }

            if(numResources > 0)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "tagAddedToResources", numResources);
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "resourcesCanNotBeChanged", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addFatalMessage(e);
        }
    }
}
