package de.l3s.learnweb.group;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.AddFolderBean;
import de.l3s.learnweb.resource.AddResourceBean;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.ResourceType;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceManager.Order;
import de.l3s.learnweb.resource.RightPaneBean;
import de.l3s.learnweb.resource.search.SearchFilters;
import de.l3s.learnweb.resource.search.SearchFilters.Filter;
import de.l3s.learnweb.resource.search.SearchFilters.MODE;
import de.l3s.learnweb.resource.search.SearchLogManager;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch.SearchPaginator;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadataSearch;
import de.l3s.learnweb.resource.yellMetadata.ExtendedMetadataSearchFilters;
import de.l3s.learnweb.user.User;
import de.l3s.util.BeanHelper;
import de.l3s.util.StringHelper;

@Named
@ViewScoped
public class GroupResourcesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = Logger.getLogger(GroupResourcesBean.class);
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Group base attributes
    private int groupId; // Current group id
    private Group group; // Current group

    private Folder selectedFolder; // Current opened folder
    private TreeNode selectedNode; // Current folder in left panel
    private List<Folder> breadcrumbs;

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
    private String[] selectedTargets;
    private String[] selectedPurposes;
    private String[] selectedLanguages;
    private String[] selectedLevels;

    private int searchLogId = -1;

    //extended metadata search/filters - authors and media sources from resources belonging to selected group only
    private List<String> authors;

    //Grid or List view of group resources
    private boolean gridView = false;

    //for extended Metadata filter search
    private ExtendedMetadataSearch extendedMetadataSearch;

    private transient SearchLogManager searchLogger;

    @Inject
    private RightPaneBean rightPaneBean;

    @Inject
    private AddResourceBean addResourceBean;

    @Inject
    private AddFolderBean addFolderBean;

    private final int pageSize;

    public GroupResourcesBean()
    {
        pageSize = getLearnweb().getProperties().getPropertyIntValue("RESOURCES_PAGE_SIZE");

        searchFilters = new SearchFilters();
        searchFilters.setMode(MODE.group);

        //updateResourcesFromSolr(); //not necessary on most pages
    }

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

            if(null == selectedFolder)
            {
                Integer id = getParameterInt("folder_id");

                if(null == id)
                {
                    selectedFolder = new Folder(0, groupId, group.getTitle());
                }
                else
                {
                    selectedFolder = getLearnweb().getGroupManager().getFolder(id);
                    buildBreadcrumbsForFolder(selectedFolder);
                }
            }
        }
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
            buildBreadcrumbsForFolder(selectedFolder);
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

    public void onSortingChanged(ValueChangeEvent e)
    {
        // TODO implement
        order = Order.TYPE;
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

        // TODO Oleh
        // TODO Dupe: duplicate exists in my SearchBean.onNodeSelect
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

    public void clearFilters()
    {
        setQuery(null);
        changeFilters(null);
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
        if(StringUtils.isEmpty(query))
        {
            this.query = null;
        }
        else if(this.query == null || !this.query.equalsIgnoreCase(query))
        {
            this.query = query;
            log(Action.group_resource_search, groupId, 0, query);
        }
    }

    public void onQueryChange() throws SQLException
    {
        updateResourcesFromSolr();
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

            if(!StringHelper.empty(query))
            {
                logQuery(query, ""); //  searchFilters.toString()
                logResources(paginator.getCurrentPage(), paginator.getPageIndex());
            }
        }
        catch(SQLException | IOException | SolrServerException e)
        {
            addErrorMessage(e);
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
        searchFilters.cleanAll();
        searchFilters.putResourceCounter(sp.getFacetFields());
        searchFilters.putResourceCounter(sp.getFacetQueries());
        return sp;
    }

    public List<Folder> getSubFolders() throws SQLException
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
            addErrorMessage(e);
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
            addErrorMessage(e);
        }
    }

    public void actionEditGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        try
        {
            String itemType = params.get("itemType");
            int itemId = StringHelper.parseInt(params.get("itemId"), -1);

            AbstractResource resource = getLearnweb().getGroupManager().getAbstractResource(itemType, itemId);
            if(resource != null && resource.canEditResource(getUser()))
            {
                rightPaneBean.setEditResource(resource);
            }
            else
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "Target folder doesn't exists or you don't have permission to edit it");
            }
        }
        catch(NullPointerException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void actionCreateGroupItem() throws IllegalAccessException, InvocationTargetException, IOException
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
        case "glossary2":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            addResourceBean.setResourceAsGlossary();
            break;
        case "survey":
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
            addResourceBean.getResource().setType(ResourceType.survey);
            break;
        case "newFile":
            ResourceType docType = ResourceType.parse(getParameter("docType"));
            rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newFile);
            addResourceBean.getResource().setType(docType);
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
            addErrorMessage(e);
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

            // TODO Oleh
            // TODO Dupe: duplicate exists in my MyResourceBean.actionCopyGroupItems
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
                        log(Action.adding_resource, targetGroupId, resource.getId());
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
            addErrorMessage(e);
        }
    }

    private void moveGroupItems(JSONArray objects, JSONObject dest)
    {
        try
        {
            int numFolders = 0, numResources = 0, numSkipped = 0, targetGroupId = selectedResourceTargetGroupId, targetFolderId = selectedResourceTargetFolderId;

            if(dest != null)
            {
                try
                {
                    targetGroupId = Integer.parseInt(dest.getString("groupId"));
                }
                catch(JSONException | NumberFormatException e)
                {
                    targetGroupId = groupId;
                }

                try
                {
                    targetFolderId = Integer.parseInt(dest.getString("folderId"));
                }
                catch(JSONException | NumberFormatException e)
                {
                    targetFolderId = 0;
                }
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

                        if(!sourceFolder.isEditPossible())
                        {
                            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceLockedByAnotherUser", sourceFolder.getLockUsername());
                            return;
                        }

                        log(Action.move_folder, sourceFolder.getGroupId(), itemId, sourceFolder.getTitle());
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

                        if(!sourceResource.isEditPossible())
                        {
                            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceLockedByAnotherUser", sourceResource.getLockUsername());
                            return;
                        }

                        log(Action.move_resource, sourceResource.getGroupId(), itemId, sourceResource.getTitle());
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
            addErrorMessage(e);
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

                        if(!folder.isEditPossible())
                        {
                            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceLockedByAnotherUser", folder.getLockUsername());
                            return;
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

                        if(!resource.isEditPossible())
                        {
                            addGrowl(FacesMessage.SEVERITY_ERROR, "resourceLockedByAnotherUser", resource.getLockUsername());
                            return;
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
            addErrorMessage(e);
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

            // TODO Oleh
            // TODO Dupe: duplicate exists in my MyResourceBean.addTagToGroupItems
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
            addErrorMessage(e);
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

    public List<String> getAuthors()
    {

        //get the list of unique authors from database
        try
        {
            if(null == this.group)
            {
                log.fatal("group must not be null; " + BeanHelper.getRequestSummary());
                return null;
            }

            authors = new ArrayList<>();
            List<Resource> resources = this.group.getResources();

            for(Resource resource : resources)
            {
                String exist = "false";

                if((resource.getAuthor() != null) && (resource.getAuthor().length() != 0))
                {

                    for(String author : authors)
                    {
                        if(author.equalsIgnoreCase(resource.getAuthor().trim()))
                        {
                            exist = "true";
                        }
                    }

                    if(exist.equals("false"))
                    {
                        authors.add(resource.getAuthor().trim());
                    }
                }
            }
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }

        return authors;
    }

    public void setAuthors(List<String> authors)
    {
        this.authors = authors;

    }

    //extended metadata filtering methods and returns filter results (paginator)
    public void onMetadataFilterClick()
    {

        emFilters = new ExtendedMetadataSearchFilters();

        emFilters.setFilterAuthors(selectedAuthors);
        emFilters.setFilterLangs(selectedLanguages);
        emFilters.setFilterLevels(selectedLevels);
        emFilters.setFilterPurposes(selectedPurposes);
        emFilters.setFilterTargets(selectedTargets);

        int folderId = (selectedFolder != null && selectedFolder.getId() > 0) ? selectedFolder.getId() : 0;

        extendedMetadataSearch = new ExtendedMetadataSearch(getUser());
        extendedMetadataSearch.setResultsPerPage(8);
        //emSearchBean.setSort("timestamp DESC");

        paginator = extendedMetadataSearch.getFilterResults(groupId, folderId, emFilters, getUser());
    }

    public void logQuery(String query, String searchFilters)
    {
        searchLogId = getSearchLogger().logGroupQuery(group, query, searchFilters, UtilBean.getUserBean().getLocaleCode(), getUser());
    }

    private void logResources(List<ResourceDecorator> resources, int pageId)
    {
        /*if(searchId > 0) // log resources only when the logQuery() was called before; This isn't the case on the group search page
            getSearchLogger().logResources(searchId, resources);*/

        //call the method to fetch the html of the logged resources
        //only if search_mode='text' and userId is admin/specificUser
        if(searchLogId > 0)
            getSearchLogger().logResources(searchLogId, resources, false, pageId);
    }

    private SearchLogManager getSearchLogger() // TODO remove just use getLearnweb().getSearchLogManager() which is already cached
    {
        if(searchLogger == null)
            searchLogger = Learnweb.getInstance().getSearchLogManager();

        return searchLogger;
    }
}
