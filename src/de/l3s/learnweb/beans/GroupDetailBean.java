package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
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
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import com.google.gson.Gson;

import de.l3s.learnweb.AbstractPaginator;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.GoogleDriveManager;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Link;
import de.l3s.learnweb.Link.LinkType;
import de.l3s.learnweb.LogEntry;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Organisation.Option;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.ResourceType;
import de.l3s.learnweb.ResourceManager.Order;
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.RightPaneBean.RightPaneAction;
import de.l3s.learnweb.rm.CategoryTree;
import de.l3s.learnweb.rm.ExtendedMetadataSearchFilters;
import de.l3s.learnweb.rm.beans.ExtendedMetadataSearch;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.learnweb.solrClient.SolrSearch.SearchPaginator;
import de.l3s.util.StringHelper;

@ManagedBean
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
    private List<LogEntry> logMessages;

    // Group edit fields (Required for editing group)
    private String editedGroupDescription;
    @NotEmpty
    @Size(min = 3, max = 60)
    private String editedGroupTitle;
    private int editedGroupLeaderId;

    private User clickedUser;

    private boolean allLogs = false;
    private boolean reloadLogs = false;
    private boolean isNewestResourceHidden = false;

    // New link form
    @NotEmpty
    private String newLinkUrl;
    @NotEmpty
    private String newLinkTitle;
    private String newLinkType;
    private String newHypothesisLink;
    private String newHypothesisToken;

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

    //extended metadata search/filters (Chloe)
    private ExtendedMetadataSearchFilters emFilters;
    private String[] selectedAuthors;
    private String[] selectedMtypes;
    private String[] selectedSources;
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String[] selectedLanguages;
    private String[] selectedLevels;
    private String selectedCatNode;

    //extended metadata search/filters - authors and media sources from resources belonging to selected group only
    private List<String> authors;
    private List<String> msources;

    //Grid or List view of group resources
    private boolean gridView = false;

    //for extended Metadata filter search
    private ExtendedMetadataSearch extendedMetadataSearch;

    //for category filter search
    private CategoryTree groupCatTree;
    private String groupCatJson; //JSONified groupCatTree for javascript function

    @ManagedProperty(value = "#{rightPaneBean}")
    private RightPaneBean rightPaneBean;

    @ManagedProperty(value = "#{addResourceBean}")
    private AddResourceBean addResourceBean;

    @ManagedProperty(value = "#{addFolderBean}")
    private AddFolderBean addFolderBean;

    @ManagedProperty(value = "#{groupSummaryBean}")
    private GroupSummaryBean groupSummaryBean;

    private final int pageSize;

    private List<LogEntry> newslist;

    public GroupDetailBean() throws SQLException
    {
        pageSize = getLearnweb().getProperties().getPropertyIntValue("RESOURCES_PAGE_SIZE");

        loadGroup();

        if(null == group)
        {
            return;
        }

        updateLinksList();

        clickedUser = new User(); // TODO initialize with null

        searchFilters = new SearchFilters();
        searchFilters.setMode(MODE.group);

        //updateResourcesFromSolr(); //not necessary on most pages
    }

    public void onLoad()
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

    public boolean isUserDetailsHidden()
    {
        User user = getUser();
        if(user == null)
            return false;
        if(user.getOrganisation().getId() == 1249 && user.getOrganisation().getOption(Option.Misc_Anonymize_usernames))
            return true;
        return false;
    }

    public List<LogEntry> getNewslist()
    {
        if(null == newslist || reloadLogs)
        {
            loadLogs(25);

            if(logMessages.size() < 25)
                allLogs = true;
        }

        return logMessages;

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
            editedGroupLeaderId = group.getLeader() == null ? 0 : group.getLeader().getId();
            editedGroupTitle = group.getTitle();
            newHypothesisLink = group.getHypothesisLink();
            newHypothesisToken = group.getHypothesisToken();

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

    public Group getGroup()
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
        getGroupSummaryBean().setGroupId(groupId);
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
            log.error("unhandled error", e);
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
                if(newLinkUrl.startsWith("https://docs.google.com"))
                {
                    type = LinkType.DOCUMENT;
                    log(Action.group_adding_document, group.getId(), group.getId(), newLinkTitle);
                }
                else
                {
                    type = LinkType.LINK;
                    log(Action.group_adding_link, group.getId(), group.getId(), newLinkTitle);
                }
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
        addResourceBean.setTargetGroupId(selectedResourceTargetGroupId);
        addResourceBean.setTargetFolderId(selectedResourceTargetFolderId);
    }

    public List<Link> getLinks() throws SQLException
    {
        if(null == links)
            updateLinksList();

        return links;
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
            //            RequestContext.getCurrentInstance().update(":filters");
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
        solrSearch.setResultsPerPage(pageSize);
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

    public Folder getSelectedFolder()
    {
        return selectedFolder;
    }

    public void setSelectedFolder(Folder folder)
    {
        selectedFolder = folder;
        updateResourcesFromSolr();
        buildBreadcrumbsForFolder(folder);

        rightPaneBean.resetPane();
    }

    public TreeNode getSelectedNode()
    {
        return selectedNode;
    }

    public void setSelectedNode(TreeNode selectedNode)
    {
        this.selectedNode = selectedNode;
    }

    public void onGroupMenuNodeSelect(NodeSelectEvent event)
    {
        if(selectedNode != null)
        {
            Folder selectedFolder = (Folder) selectedNode.getData();
            setSelectedFolder(selectedFolder);
        }
        else
        {
            log.error("selectedNode is null on onNodeSelect called.", new Exception());
        }
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
            if(newHypothesisLink != group.getHypothesisLink())
            {
                group.setHypothesisLink(newHypothesisLink);
                log(Action.group_changing_leader, group.getId(), group.getId());
            }
            if(newHypothesisToken != group.getHypothesisToken())
            {
                group.setHypothesisToken(newHypothesisToken);
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

    public RightPaneBean getRightPaneBean()
    {
        return rightPaneBean;
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
    }

    public AddResourceBean getAddResourceBean()
    {
        return addResourceBean;
    }

    public void setAddResourceBean(AddResourceBean addResourceBean)
    {
        this.addResourceBean = addResourceBean;
    }

    public AddFolderBean getAddFolderBean()
    {
        return addFolderBean;
    }

    public void setAddFolderBean(AddFolderBean addFolderBean)
    {
        this.addFolderBean = addFolderBean;
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
                    rightPaneBean.setViewResource(folder);
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                {
                    rightPaneBean.setViewResource(resource);
                }
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
                if(folder != null && folder.canEditResource(getUser()))
                {
                    rightPaneBean.setViewResource(folder);
                    rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.editFolder);
                }
                else
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "Target folder doesn't exists or you don't have permission to edit it");
                }
            }
            else if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null && resource.canEditResource(getUser()))
                {
                    rightPaneBean.setViewResource(resource);
                    rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.editResource);
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
        String type = getParameter("type");

        // Set target group and folder in beans
        switch(type)
        {
        case "folder":
            addFolderBean.clearForm();
            addFolderBean.setTargetGroup(group);
            addFolderBean.setTargetFolder(selectedFolder);
            break;
        default:
            addResourceBean.clearForm();
            addResourceBean.setTargetGroup(group);
            addResourceBean.setTargetFolder(selectedFolder);
            addResourceBean.getResource().setStorageType(Resource.LEARNWEB_RESOURCE);
            break;
        }

        // Set target view and defaults
        switch(type)
        {
        case "folder":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newFolder);
            break;
        case "file":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            break;
        case "url":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            addResourceBean.getResource().setStorageType(Resource.WEB_RESOURCE);
            break;
        case "glossary":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            addResourceBean.getResource().setType(ResourceType.glossary);
            break;
        case "survey":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            addResourceBean.getResource().setType(ResourceType.survey);
            break;
        case "newFile":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newFile);
            addResourceBean.getResource().setType(getParameter("docType"));
            break;
        default:
            log.warn("Unsupported item type: " + type);
            break;
        }
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

            if(!getGroup().canViewResources(getUser()))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to copy this resource.");
                return;
            }

            if(targetGroupId != 0)
            {
                Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
                if(targetGroup == null)
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "Target group is wrong and not exists.");
                    return;
                }

                if(!targetGroup.canAddResources(getUser()))
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to add resources to target group.");
                    return;
                }
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
                        if(resource.getType().equals(Resource.ResourceType.survey))
                        {
                            getLearnweb().getCreateSurveyManager().copySurveyResource(itemId, newResource.getId());
                        }
                        else if(resource.getType().equals(Resource.ResourceType.glossary))
                        {
                            getLearnweb().getGlossariesManager().copyGlossary(itemId, newResource.getId());
                        }
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

            if(targetGroupId != 0)
            {
                Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
                if(!targetGroup.canAddResources(getUser()))
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to add resources to target group.");
                    return;
                }
            }

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);
                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"));
                if(itemType != null && itemType.equals("folder") && itemId > 0)
                {
                    Folder sourceFolder = getLearnweb().getGroupManager().getFolder(itemId);
                    if(sourceFolder != null)
                    {
                        if(!sourceFolder.canDeleteResource(getUser()))
                        {
                            numSkipped++;
                            log.warn("The user don't have permissions to delete folder which it want to move.");
                            continue;
                        }

                        sourceFolder.moveTo(targetGroupId, targetFolderId);
                        numFolders++;
                    }
                    else
                    {
                        numSkipped++;
                        log.warn("Source folder does not exists on actionMoveGroupItems");
                    }
                }
                else if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource sourceResource = getLearnweb().getResourceManager().getResource(itemId);
                    if(sourceResource != null)
                    {
                        if(!sourceResource.canDeleteResource(getUser()))
                        {
                            numSkipped++;
                            log.warn("The user don't have permissions to delete resource which it want to move.");
                            continue;
                        }

                        sourceResource.moveTo(targetGroupId, targetFolderId);
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
                        if(!folder.canDeleteResource(getUser()))
                        {
                            numSkipped++;
                            log.warn("The user don't have permissions to delete folder in target group.");
                            continue;
                        }

                        int folderGroupId = folder.getGroupId();
                        String folderName = folder.getTitle();
                        if(rightPaneBean.isTheResourceClicked(folder))
                            rightPaneBean.resetPane();

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
                        if(rightPaneBean.isTheResourceClicked(resource))
                            rightPaneBean.resetPane();

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
                {
                    this.updateResourcesFromSolr();
                    rightPaneBean.resetPane();
                }
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
            if(!getGroup().canAnnotateResources(getUser()))
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

    //allow switching between grid and list view of group resources - chloe
    public boolean isGridView()
    {
        return gridView;
    }

    public void setGridView(boolean gridView)
    {
        this.gridView = gridView;
    }

    //metadata filter search variables and methods to be called from resources_yell.xhtml
    //setter and getter for extended metadata search variables

    public ExtendedMetadataSearchFilters getEmFilters()
    {
        return emFilters;
    }

    public void setEmFilters(ExtendedMetadataSearchFilters emFilters)
    {
        this.emFilters = emFilters;
    }

    public String[] getSelectedAuthors()
    {
        return selectedAuthors;
    }

    public void setSelectedAuthors(String[] selectedAuthors)
    {
        this.selectedAuthors = selectedAuthors;
    }

    public String[] getSelectedMtypes()
    {
        return selectedMtypes;
    }

    public void setSelectedMtypes(String[] selectedMtypes)
    {
        this.selectedMtypes = selectedMtypes;
    }

    public String[] getSelectedSources()
    {
        return selectedSources;
    }

    public void setSelectedSources(String[] selectedSources)
    {
        this.selectedSources = selectedSources;
    }

    public String[] getSelectedTargets()
    {
        return selectedTargets;
    }

    public void setSelectedTargets(String[] selectedTargets)
    {
        this.selectedTargets = selectedTargets;
    }

    public String[] getSelectedPurposes()
    {
        return selectedPurposes;
    }

    public void setSelectedPurposes(String[] selectedPurposes)
    {
        this.selectedPurposes = selectedPurposes;
    }

    public String[] getSelectedLanguages()
    {
        return selectedLanguages;
    }

    public void setSelectedLanguages(String[] selectedLanguages)
    {
        this.selectedLanguages = selectedLanguages;
    }

    public String[] getSelectedLevels()
    {
        return selectedLevels;
    }

    public void setSelectedLevels(String[] selectedLevels)
    {
        this.selectedLevels = selectedLevels;
    }

    public String getSelectedCatNode()
    {
        return selectedCatNode;
    }

    public void setSelectedCatNode(String selectedCatNode)
    {
        this.selectedCatNode = selectedCatNode;
    }

    public List<String> getAuthors()
    {

        //get the list of unique authors from database
        try
        {
            authors = new ArrayList<String>();
            List<Resource> gresources = this.group.getResources();

            for(int i = 0; i < gresources.size(); i++)
            {
                String exist = "false";

                if((gresources.get(i).getAuthor() != null) && (gresources.get(i).getAuthor().length() != 0))
                {

                    for(int j = 0; j < authors.size(); j++)
                    {
                        if(authors.get(j).equalsIgnoreCase(gresources.get(i).getAuthor().trim()))
                        {
                            exist = "true";
                        }
                    }

                    if(exist.equals("false"))
                    {
                        authors.add(gresources.get(i).getAuthor().trim());
                    }
                }
            }
        }
        catch(SQLException e)
        {
            log.debug(e);
        }

        return authors;
    }

    public void setAuthors(List<String> authors)
    {
        this.authors = authors;

    }

    public List<String> getMsources()
    {
        //get the list of unique media sources from database
        try
        {
            msources = new ArrayList<String>();
            List<Resource> gresources = this.group.getResources();

            for(int i = 0; i < gresources.size(); i++)
            {
                String exist = "false";

                if((gresources.get(i).getMsource() != null) && (gresources.get(i).getMsource().length() != 0))
                {

                    for(int j = 0; j < msources.size(); j++)
                    {
                        if(msources.get(j).equalsIgnoreCase(gresources.get(i).getMsource().trim()))
                        {
                            exist = "true";
                        }
                    }

                    if(exist.equals("false"))
                    {
                        msources.add(gresources.get(i).getMsource().trim());
                    }
                }
            }
        }
        catch(SQLException e)
        {
            log.debug(e);
        }

        return msources;
    }

    public void setMsources(List<String> msources)
    {
        this.msources = msources;
    }

    public CategoryTree getGroupCatTree() throws SQLException
    {
        if(groupCatTree == null)
        {
            groupCatTree = createGroupCatTree(this.group.getResources());
        }
        return groupCatTree;
    }

    private CategoryTree createGroupCatTree(List<Resource> resources) throws SQLException
    {
        CategoryTree cattree;

        cattree = new CategoryTree(resources);

        return cattree;
    }

    public String getGroupCatJson() throws SQLException
    {
        if(groupCatJson == null)
        {
            this.groupCatTree = getGroupCatTree();
        }

        Gson gson = new Gson();
        this.groupCatJson = gson.toJson(this.groupCatTree);

        return groupCatJson;
    }

    public void setGroupCatJson(String groupCatJson)
    {
        this.groupCatJson = groupCatJson;
    }

    public void setGroupCatTree(CategoryTree groupCatTree)
    {
        this.groupCatTree = groupCatTree;
    }

    //category filtering method called from javascript(Learnweb_chloe_v2.js) via remotecommand
    public void onCategoryFilterClick() throws SQLException
    {
        extendedMetadataSearch = new ExtendedMetadataSearch(getUser());
        extendedMetadataSearch.setResultsPerPage(8);

        String catname = getParameter("catname");
        String catlevel = getParameter("catlevel");

        this.selectedCatNode = catname;

        List<Resource> gResources = group.getResources();

        paginator = extendedMetadataSearch.getCatFilterResults(gResources, catname, catlevel);

        log(Action.group_category_search, groupId, 0, catname);
    }

    //extended metadata filtering methods and returns filter results (paginator)
    public void onMetadataFilterClick()
    {

        emFilters = new ExtendedMetadataSearchFilters();

        emFilters.setFilterAuthors(selectedAuthors);
        emFilters.setFilterLangs(selectedLanguages);
        emFilters.setFilterLevels(selectedLevels);
        emFilters.setFilterMtypes(selectedMtypes);
        emFilters.setFilterPurposes(selectedPurposes);
        emFilters.setFilterTargets(selectedTargets);
        emFilters.setFilterSources(selectedSources);

        int folderId = (selectedFolder != null && selectedFolder.getId() > 0) ? selectedFolder.getId() : 0;

        extendedMetadataSearch = new ExtendedMetadataSearch(getUser());
        extendedMetadataSearch.setResultsPerPage(8);
        //emSearchBean.setSort("timestamp DESC");

        paginator = extendedMetadataSearch.getFilterResults(groupId, folderId, emFilters, getUser());
    }

    public void displayClickedResourceFromSlider() throws SQLException
    {
        SimpleEntry<String, Resource> clickedResourceFromSlider = groupSummaryBean.getChoosenResourceFromSlider();
        if(clickedResourceFromSlider != null)
        {
            rightPaneBean.setPaneAction("updated".equals(clickedResourceFromSlider.getKey()) ? RightPaneAction.viewUpdatedResource : RightPaneAction.viewResource);
            rightPaneBean.setClickedAbstractResource(clickedResourceFromSlider.getValue());
        }
    }

    public String getNewHypothesisLink()
    {
        return newHypothesisLink;
    }

    public void setNewHypothesisLink(String newHypothesisLink)
    {
        this.newHypothesisLink = newHypothesisLink;
    }

    public String getNewHypothesisToken()
    {
        return newHypothesisToken;
    }

    public void setNewHypothesisToken(String newHypothesisToken)
    {
        this.newHypothesisToken = newHypothesisToken;
    }

    public GroupSummaryBean getGroupSummaryBean()
    {
        return groupSummaryBean;
    }

    public void setGroupSummaryBean(GroupSummaryBean groupSummaryBean)
    {
        this.groupSummaryBean = groupSummaryBean;
    }

}
