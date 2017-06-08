package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.AbstractPaginator;
import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupItem;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.ResourceManager.Order;
import de.l3s.learnweb.SearchFilters;
import de.l3s.learnweb.SearchFilters.Filter;
import de.l3s.learnweb.SearchFilters.MODE;
import de.l3s.learnweb.User;
import de.l3s.learnweb.solrClient.SolrSearch;
import de.l3s.learnweb.solrClient.SolrSearch.SearchPaginator;
import de.l3s.util.StringHelper;

@ManagedBean
@ViewScoped
public class AdvSearchBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = Logger.getLogger(AdvSearchBean.class);
    private static final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private GroupItem clickedGroupItem; // Preview of resource/folder
    private RPAction rightPanelAction = RPAction.none;

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
    private final int pageSize;

    private Map<String, String> queries = new HashMap<>();
    private Map<String, String[]> queriesMultiValued = new HashMap<>();

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

    public AdvSearchBean() throws SQLException, IOException, SolrServerException
    {
        pageSize = 10;

        searchFilters = new SearchFilters();
        searchFilters.setMode(MODE.group);

        getResourcesFromSolr(896, 0, null, getUser());
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

    public Map<String, String> getQueries()
    {
        return queries;
    }

    public void setQueries(Map<String, String> queries)
    {
        this.queries = queries;
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
        return false;// canDeleteResourcesInGroup(getGroup());
    }

    public boolean canEditResourcesInGroup(Group group) throws SQLException
    {
        return group.canEditResources(getUser());
    }

    public boolean canEditResourcesInTheGroup() throws SQLException
    {
        return false;// canEditResourcesInGroup(getGroup());
    }

    public boolean canCopyResourcesFromTheGroup() throws SQLException
    {
        return canSeeResourcesInTheGroup();
    }

    public boolean canSeeResourcesInTheGroup() throws SQLException
    {
        return false;// getGroup().canViewResources(getUser());
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

    public Map<String, String[]> getQueriesMultiValued()
    {
        return queriesMultiValued;
    }

    public void setQueriesMultiValued(Map<String, String[]> queriesMultiValued)
    {
        this.queriesMultiValued = queriesMultiValued;
    }

    public void onQueryChange() throws SQLException
    {
        updateResourcesFromSolr();
        log(Action.group_resource_search, 0, 0, query);
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

        int folderId = 0;
        int groupId = 896; // TODO this is just a test
        try
        {
            paginator = getResourcesFromSolr(groupId, folderId, query, getUser());
        }
        catch(SQLException | IOException | SolrServerException e)
        {
            addFatalMessage(e);
        }
    }

    public void onSearch()
    {
        log.debug(queries);
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

    public static long getSerialVersionUID()
    {
        return serialVersionUID;
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
            /*
            case "move":
            JSONObject dest = params.containsKey("destination") ? new JSONObject(params.get("destination")) : null;
            this.moveGroupItems(items, dest);
            break;
            case "delete":
            this.deleteGroupItems(items);
            break;
            */
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
