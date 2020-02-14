package de.l3s.learnweb.group;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.AddFolderBean;
import de.l3s.learnweb.resource.AddResourceBean;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ResourceUpdateBatch;
import de.l3s.learnweb.resource.RightPaneBean;
import de.l3s.learnweb.resource.SelectLocationBean;
import de.l3s.learnweb.resource.search.SearchFilters;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.solrClient.SolrPaginator;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

@Named
@ViewScoped
public class GroupResourcesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = Logger.getLogger(GroupResourcesBean.class);
    private final DateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Group base attributes
    private Group group; // current group
    private Folder currentFolder;

    // Grid or List view of group resources
    private boolean gridView = true; // TODO: should be changed to enum
    private final int pageSize;

    // In group search/filters
    private String query;
    private SearchFilters searchFilters;

    // resources
    private transient AbstractPaginator paginator;
    private transient List<Folder> folders;
    private transient List<Folder> breadcrumbs;
    private transient TreeNode foldersTree;
    private transient TreeNode selectedTreeNode; // Selected node in the left Folder's panel

    @Inject
    private RightPaneBean rightPaneBean;

    @Inject
    private AddFolderBean addFolderBean;

    @Inject
    private AddResourceBean addResourceBean;

    @Inject
    private SelectLocationBean selectLocationBean;

    public GroupResourcesBean()
    {
        pageSize = getLearnweb().getProperties().getPropertyIntValue("RESOURCES_PAGE_SIZE");

        searchFilters = new SearchFilters();
        searchFilters.setMode(SearchMode.group);
    }

    public void resetResources()
    {
        paginator = null;
        folders = null;
        breadcrumbs = null;
        foldersTree = null;
    }

    public void setGroupId(int groupId)
    {
        try
        {
            group = getLearnweb().getGroupManager().getGroupById(groupId);
        }
        catch(SQLException e)
        {
            addInvalidParameterMessage("group_id");
        }
    }

    public int getGroupId()
    {
        return HasId.getIdOrDefault(group, 0);
    }

    public void setFolderId(int folderId)
    {
        try
        {
            if(folderId > 0) currentFolder = getLearnweb().getGroupManager().getFolder(folderId);
        }
        catch(SQLException e)
        {
            addInvalidParameterMessage("folder_id");
        }
    }

    public int getFolderId()
    {
        return HasId.getIdOrDefault(currentFolder, 0);
    }

    public void onLoad() throws SQLException
    {
        User user = getUser();
        if(null == user) return;

        if(user.getOrganisation().getId() == 480)
        {
            gridView = false;
        }

        if(null != group)
        {
            user.setActiveGroup(group);
            group.setLastVisit(user);
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

    public AbstractPaginator getPaginator()
    {
        if(null == paginator)
        {
            try
            {
                paginator = getResourcesFromSolr(group.getId(), HasId.getIdOrDefault(currentFolder, 0), query, getUser());
            }
            catch(SQLException | IOException | SolrServerException e)
            {
                addErrorMessage(e);
            }
        }

        return paginator;
    }

    public String changeFilters(String queryFilters)
    {
        searchFilters.setFiltersFromString(queryFilters);
        resetResources();
        return queryFilters;
    }

    public void clearFilters()
    {
        setQuery(null);
        changeFilters(null);
    }

    public void onQueryChange()
    {
        resetResources();
    }

    public List<Filter> getAvailableFilters()
    {
        getPaginator();
        if(searchFilters == null) // should only happen for private resources
            return null;

        return searchFilters.getAvailableFilters();
    }

    private SolrPaginator getResourcesFromSolr(int groupId, int folderId, String query, User user) throws SQLException, IOException, SolrServerException
    {
        SolrSearch solrSearch = new SolrSearch(StringUtils.isEmpty(query) ? "*" : query, user);
        solrSearch.setFilterGroups(groupId);
        solrSearch.setFilterFolder(folderId, !StringUtils.isEmpty(query));
        solrSearch.setResultsPerPage(pageSize);
        solrSearch.setSkipResourcesWithoutThumbnails(false);
        solrSearch.setFacetFields(searchFilters.getFacetFields());
        solrSearch.setFacetQueries(searchFilters.getFacetQueries());
        solrSearch.setOrder("timestamp", SolrQuery.ORDER.desc);

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

        SolrPaginator sp = new SolrPaginator(solrSearch);
        // searchFilters.cleanAll();
        searchFilters.putResourceCounter(sp.getFacetFields());
        searchFilters.putResourceCounter(sp.getFacetQueries());
        return sp;
    }

    public List<Folder> getSubFolders() throws SQLException
    {
        if (folders == null)
        {
            folders = getLearnweb().getGroupManager().getFolders(group.getId(), HasId.getIdOrDefault(currentFolder, 0));
        }
        return folders;
    }

    public TreeNode getFoldersTree(Group group) throws SQLException
    {
        if (foldersTree == null)
        {
            foldersTree = getLearnweb().getGroupManager().getFoldersTree(group, HasId.getIdOrDefault(currentFolder, 0));
        }
        return foldersTree;
    }

    public List<Folder> getBreadcrumbs()
    {
        if (breadcrumbs == null && currentFolder != null)
        {
            breadcrumbs = new ArrayList<>();
            try
            {
                Folder folder = currentFolder;
                while (folder != null && folder.getId() > 0)
                {
                    breadcrumbs.add(0, folder);
                    folder = folder.getParentFolder();
                }
            }
            catch(SQLException e)
            {
                log.warn("Can't build breadcrumbs", e);
            }
        }

        return breadcrumbs;
    }

    public void commandOpenFolder()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            int folderId = Integer.parseInt(params.get("folderId"));
            if (folderId == 0)
            {
                this.currentFolder = null;
            }
            else
            {
                Folder targetFolder = getLearnweb().getGroupManager().getFolder(folderId);
                if(targetFolder == null) throw new IllegalArgumentException("Target folder does not exists.");

                this.currentFolder = targetFolder;
            }

            resetResources();
            rightPaneBean.resetPane();
        }
        catch(IllegalArgumentException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void commandSelectResource()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String itemType = params.get("itemType");
            int itemId = Integer.parseInt(params.get("itemId"));

            if("folder".equals(itemType))
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(itemId);
                if(folder != null) rightPaneBean.setViewResource(folder);
                else throw new IllegalArgumentException("Target folder does not exists!");
            }
            else if("resource".equals(itemType))
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null) rightPaneBean.setViewResource(resource);
                else throw new IllegalArgumentException("Target resource does not exists!");
            }
            else throw new IllegalArgumentException("Unsupported element type!");
        }
        catch(IllegalArgumentException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void commandEditResource()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String itemType = params.get("itemType");
            int itemId = Integer.parseInt(params.get("itemId"));

            AbstractResource resource = getLearnweb().getGroupManager().getAbstractResource(itemType, itemId);
            if(resource != null && resource.canEditResource(getUser())) rightPaneBean.setEditResource(resource);
            else addGrowl(FacesMessage.SEVERITY_ERROR, "Target folder doesn't exists or you don't have permission to edit it.");
        }
        catch(IllegalArgumentException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void commandCreateResource()
    {
        String type = getParameter("type");

        // Set target group and folder in beans
        addResourceBean.reset();
        addResourceBean.setTarget(group, currentFolder);

        // Set target view and defaults
        switch(type)
        {
            case "folder":
                addFolderBean.reset();
                addFolderBean.setTarget(group, currentFolder);
                rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newFolder);
                break;
            case "file":
                rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
                addResourceBean.getResource().setType(ResourceType.file);
                break;
            case "url":
                rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
                addResourceBean.getResource().setType(ResourceType.website);
                addResourceBean.getResource().setStorageType(Resource.WEB_RESOURCE);
                break;
            case "glossary2":
                rightPaneBean.setPaneAction(RightPaneBean.RightPaneAction.newResource);
                addResourceBean.setResourceTypeGlossary();
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
                log.error("Unsupported item type: " + type);
                break;
        }
    }

    public void commandBatchUpdateResources()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String action = params.get("action");
            ResourceUpdateBatch items = new ResourceUpdateBatch(params.get("items"));

            switch(action)
            {
                case "copy":
                    this.copyResources(items);
                    break;
                case "move":
                    if(params.containsKey("destination"))
                    {
                        JSONObject dest = new JSONObject(params.get("destination"));
                        int targetGroupId = dest.isNull("groupId") ? group.getId() : dest.getInt("groupId");
                        int targetFolderId = dest.isNull("folderId") ? 0 : dest.getInt("folderId");
                        this.moveResources(items, targetGroupId, targetFolderId);
                        break;
                    }

                    this.moveResources(items, null, null);
                    break;
                case "delete":
                    this.deleteResources(items);
                    break;
                case "add-tag":
                    String tag = params.get("tag");
                    this.tagResources(items, tag);
                    break;
                default:
                    log.error("Unsupported action: " + action);
                    break;
            }

            if(items.getFailed() > 0) addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.cant_be_processed", items.getFailed());
        }
        catch(IllegalArgumentException | IllegalAccessError e)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
        catch(JSONException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    private void copyResources(ResourceUpdateBatch items) throws SQLException
    {
        Group targetGroup = selectLocationBean.getTargetGroup();
        if(targetGroup == null) throw new IllegalArgumentException("group_resources.target_not_exists");
        if(!group.canViewResources(getUser())) throw new IllegalAccessError("group_resources.cant_be_copied");
        if(!targetGroup.canAddResources(getUser())) throw new IllegalAccessError("group_resources.target_permissions");

        for(Resource resource : items.getResources())
        {
            Resource newResource = resource.clone();
            newResource.setGroupId(HasId.getIdOrDefault(selectLocationBean.getTargetGroup(), 0));
            newResource.setFolderId(HasId.getIdOrDefault(selectLocationBean.getTargetFolder(), 0));
            resource = getUser().addResource(newResource);
            log(Action.adding_resource, targetGroup.getId(), resource.getId());
        }

        if (!items.getFolders().isEmpty())
        {
            // TODO: implement copy folder
            addGrowl(FacesMessage.SEVERITY_WARN, "Copying folders is not implemented yet.");
        }

        if(items.getTotal() > 0) addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.copied_successfully", items.getTotal());
    }

    private void moveResources(ResourceUpdateBatch items, Integer targetGroupId, Integer targetFolderId) throws SQLException
    {
        int skipped = 0;
        if(targetGroupId == null)
        {
            if (selectLocationBean.getTargetGroup() != null)
            {
                targetGroupId = selectLocationBean.getTargetGroup().getId();
                targetFolderId = selectLocationBean.getTargetFolder().getId();
            }
            else throw new IllegalArgumentException("group_resources.target_not_exists");
        }

        if(targetGroupId != 0)
        {
            Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
            if(!targetGroup.canAddResources(getUser())) throw new IllegalAccessError("group_resources.target_permissions");
        }

        for(Folder folder : items.getFolders())
        {
            if(isDeleteRestricted(folder))
            {
                skipped++;
                continue;
            }

            folder.moveTo(targetGroupId, targetFolderId);

            log(Action.move_folder, folder.getGroupId(), folder.getId(), folder.getTitle());
        }

        for(Resource resource : items.getResources())
        {
            if(isDeleteRestricted(resource))
            {
                skipped++;
                continue;
            }

            resource.moveTo(targetGroupId, targetFolderId);

            log(Action.move_resource, resource.getGroupId(), resource.getId(), resource.getTitle());
        }

        if(skipped > 0) addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        if(items.getTotal() - skipped > 0)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.moved_successfully", items.getTotal() - skipped);
            resetResources();
        }
    }

    private void deleteResources(ResourceUpdateBatch items) throws SQLException
    {
        int skipped = 0;

        for(Folder folder : items.getFolders())
        {
            if(isDeleteRestricted(folder))
            {
                skipped++;
                continue;
            }

            folder.delete();
            log(Action.deleting_folder, folder.getGroupId(), folder.getId(), folder.getTitle());

            if(rightPaneBean.isTheResourceClicked(folder)) rightPaneBean.resetPane();
            if(folder.equals(currentFolder)) currentFolder = null;
        }

        for(Resource resource : items.getResources())
        {
            if(isDeleteRestricted(resource))
            {
                skipped++;
                continue;
            }

            resource.delete();
            log(Action.deleting_resource, resource.getGroupId(), resource.getId(), resource.getTitle());

            if(rightPaneBean.isTheResourceClicked(resource)) rightPaneBean.resetPane();
        }

        if(items.getTotal() - skipped > 0)
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.deleted_successfully", items.getTotal() - skipped);
            resetResources();
        }

        if(skipped > 0)
        {
            addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        }
    }

    private void tagResources(ResourceUpdateBatch items, String tag) throws SQLException
    {
        int skipped = 0;
        for(Resource resource : items.getResources())
        {
            if(!resource.canAnnotateResource(getUser()))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.denied_annotate", resource.getTitle());
                skipped++;
                continue;
            }

            resource.addTag(tag, getUser());
            log(Action.tagging_resource, resource.getGroupId(), resource.getId(), tag);
        }

        if(!items.getResources().isEmpty())
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.annotated_successfully", items.getResources().size());
        }

        if(skipped > 0)
        {
            addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        }
    }

    private boolean isDeleteRestricted(AbstractResource resource) throws SQLException
    {
        if(!resource.isEditPossible())
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.denied_locked", resource.getTitle());
            return true;
        }

        if(!resource.canDeleteResource(getUser()))
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.denied_delete", resource.getTitle());
            return true;
        }

        return false;
    }

    /* ------------------------ Properties getters/setters ------------------------ */

    public Group getGroup()
    {
        return group;
    }

    public Folder getCurrentFolder()
    {
        return currentFolder;
    }

    public TreeNode getSelectedTreeNode()
    {
        return selectedTreeNode;
    }

    public void setSelectedTreeNode(TreeNode selectedTreeNode)
    {
        this.selectedTreeNode = selectedTreeNode;
    }

    public boolean isGridView()
    {
        return gridView;
    }

    public void setGridView(boolean gridView)
    {
        this.gridView = gridView;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        if(StringUtils.isBlank(query))
        {
            this.query = null;
        }
        else if(!query.equalsIgnoreCase(this.query))
        {
            this.query = query;
            log(Action.group_resource_search, group.getId(), 0, query);
        }
    }

    public SearchFilters getSearchFilters()
    {
        return searchFilters;
    }

    public void setSearchFilters(SearchFilters searchFilters)
    {
        this.searchFilters = searchFilters;
    }

    /* ------------------------ Beans getters/setters ------------------------ */

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

    public RightPaneBean getRightPaneBean()
    {
        return rightPaneBean;
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
    }

    public SelectLocationBean getSelectLocationBean()
    {
        return selectLocationBean;
    }

    public void setSelectLocationBean(final SelectLocationBean selectLocationBean)
    {
        this.selectLocationBean = selectLocationBean;
    }
}
