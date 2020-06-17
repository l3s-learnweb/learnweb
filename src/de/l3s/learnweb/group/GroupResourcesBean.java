package de.l3s.learnweb.group;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.omnifaces.util.Beans;
import org.primefaces.model.TreeNode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceUpdateBatch;
import de.l3s.learnweb.resource.SelectLocationBean;
import de.l3s.learnweb.resource.search.SearchFilters;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.filters.FilterType;
import de.l3s.learnweb.resource.search.solrClient.SolrPaginator;
import de.l3s.learnweb.resource.search.solrClient.SolrSearch;
import de.l3s.learnweb.user.User;
import de.l3s.util.HasId;

@Named
@ViewScoped
public class GroupResourcesBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -9105093690086624246L;
    private static final Logger log = LogManager.getLogger(GroupResourcesBean.class);

    private static final DateTimeFormatter SOLR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private enum ResourceView {
        grid,
        table,
        list
    }

    // bean params
    private int groupId;
    private int folderId;

    // Group base attributes
    private Group group; // current group
    private Folder currentFolder;

    // Grid or List view of group resources
    private ResourceView view = ResourceView.grid;
    private boolean showFoldersTree = false;
    private final int pageSize;

    // In group search/filters
    private String searchQuery;
    private final SearchFilters searchFilters = new SearchFilters(SearchMode.group);

    // resources
    private transient AbstractPaginator paginator;
    private transient List<Folder> folders;
    private transient List<Folder> breadcrumbs;
    private transient TreeNode foldersTree;
    private TreeNode selectedTreeNode; // Selected node in the left Folder's panel

    public GroupResourcesBean() {
        pageSize = getLearnweb().getProperties().getPropertyIntValue("RESOURCES_PAGE_SIZE");
    }

    public void clearCaches() {
        paginator = null;
        breadcrumbs = null;

        clearFoldersCaches();
    }

    public void clearFoldersCaches() {
        if (group != null) {
            group.clearCaches();
        }
        if (currentFolder != null) {
            currentFolder.clearCaches();
        }

        folders = null;
        foldersTree = null;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public void onLoad() throws SQLException {
        User user = getUser();
        if (null == user) {
            return;
        }

        if (user.getOrganisation().getId() == 480) {
            view = ResourceView.list;
        }

        if (groupId > 0) {
            group = getLearnweb().getGroupManager().getGroupById(groupId);

            if (null == group) {
                addInvalidParameterMessage("group_id");
                return;
            }

            if (!group.canViewResources(getUser())) {
                addAccessDeniedMessage();
                return;
            }
            group.setLastVisit(user);
        } else {
            group = new PrivateGroup(getLocaleMessage("myPrivateResources"), getUser());
        }

        if (folderId > 0) {
            currentFolder = getLearnweb().getGroupManager().getFolder(folderId);

            if (null == currentFolder) {
                addInvalidParameterMessage("folder_id");
            }
        }
    }

    public boolean isMember() throws SQLException {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (null == group) {
            return false;
        }

        return group.isMember(user);
    }

    public AbstractPaginator getPaginator() {
        if (null == paginator) {
            try {
                paginator = getResourcesFromSolr(group.getId(), HasId.getIdOrDefault(currentFolder, 0), searchQuery, getUser());
            } catch (SQLException | IOException | SolrServerException e) {
                addErrorMessage(e);
            }
        }

        return paginator;
    }

    public void changeFilter(FilterType type, String filterValue) {
        searchFilters.setFilter(type, filterValue);
        clearCaches();
    }

    public void clearFilters() {
        setSearchQuery(null);
        searchFilters.reset();
        paginator = null;
    }

    public void onQueryChange() {
        clearCaches();
    }

    public Collection<Filter> getAvailableFilters() {
        getPaginator();
        return searchFilters.getAvailableFilters();
    }

    public boolean isFiltersActive() {
        return searchFilters.isFiltersActive();
    }

    private SolrPaginator getResourcesFromSolr(int groupId, int folderId, String query, User user) throws SQLException, IOException, SolrServerException {
        SolrSearch solrSearch = new SolrSearch(StringUtils.isEmpty(query) ? "*" : query, user);
        solrSearch.setFilterGroups(groupId);
        solrSearch.setFilterFolder(folderId, !StringUtils.isEmpty(query));
        solrSearch.setResultsPerPage(pageSize);
        solrSearch.setSkipResourcesWithoutThumbnails(false);
        solrSearch.setFacetFields(searchFilters.getFacetFields());
        solrSearch.setFacetQueries(searchFilters.getFacetQueries());
        solrSearch.setOrder("timestamp", SolrQuery.ORDER.desc);

        if (searchFilters.isFilterActive(FilterType.service)) {
            solrSearch.setFilterLocation(searchFilters.getFilterValue(FilterType.service));
        }
        if (searchFilters.isFilterActive(FilterType.type)) {
            solrSearch.setFilterType(searchFilters.getFilterValue(FilterType.type));
        }
        if (searchFilters.getFilterDateFrom() != null) {
            solrSearch.setFilterDateFrom(SOLR_DATE_FORMAT.format(searchFilters.getFilterDateFrom()));
        }
        if (searchFilters.getFilterDateTo() != null) {
            solrSearch.setFilterDateTo(SOLR_DATE_FORMAT.format(searchFilters.getFilterDateTo()));
        }
        if (searchFilters.isFilterActive(FilterType.author)) {
            solrSearch.setFilterAuthor(searchFilters.getFilterValue(FilterType.author));
        }

        if (searchFilters.isFilterActive(FilterType.tags)) {
            solrSearch.setFilterTags(searchFilters.getFilterValue(FilterType.tags));
        }

        // TODO @Oleh these filters are specific for archiveweb which doesn't exist any more. Couldn't it be generalized for the fields defined per organisation?
        if (searchFilters.isFilterActive(FilterType.coverage)) {
            solrSearch.setFilterCoverage(searchFilters.getFilterValue(FilterType.coverage));
        }
        if (searchFilters.isFilterActive(FilterType.publisher)) {
            solrSearch.setFilterPublisher(searchFilters.getFilterValue(FilterType.publisher));
        }
        if (searchFilters.isFilterActive(FilterType.collector)) {
            solrSearch.setFilterCollector(searchFilters.getFilterValue(FilterType.collector));
        }
        if (searchFilters.isFilterActive(FilterType.language_level)) {
            solrSearch.setFilterLanguageLevel(searchFilters.getFilterValue(FilterType.language_level));
        }
        if (searchFilters.isFilterActive(FilterType.yell_purpose)) {
            solrSearch.setFilterYellPurpose(searchFilters.getFilterValue(FilterType.yell_purpose));
        }
        if (searchFilters.isFilterActive(FilterType.yell_target)) {
            solrSearch.setFilterYellTarget(searchFilters.getFilterValue(FilterType.yell_target));
        }
        if (searchFilters.isFilterActive(FilterType.language)) {
            solrSearch.setFilterLanguage(searchFilters.getFilterValue(FilterType.language));
        }

        SolrPaginator sp = new SolrPaginator(solrSearch);
        searchFilters.resetCounters();
        searchFilters.putResourceCounters(sp.getFacetFields());
        searchFilters.putResourceCounters(sp.getFacetQueries());
        return sp;
    }

    public List<Folder> getSubFolders() throws SQLException {
        if (folders == null) {
            folders = currentFolder == null ? group.getSubFolders() : currentFolder.getSubFolders();
        }
        return folders;
    }

    public TreeNode getFoldersTree() throws SQLException {
        if (foldersTree == null) {
            foldersTree = getLearnweb().getGroupManager().getFoldersTree(group, HasId.getIdOrDefault(currentFolder, 0));
        }
        return foldersTree;
    }

    public List<Folder> getBreadcrumbs() {
        if (breadcrumbs == null && currentFolder != null) {
            breadcrumbs = new ArrayList<>();
            try {
                Folder folder = currentFolder;
                while (folder != null && folder.getId() > 0) {
                    breadcrumbs.add(0, folder);
                    folder = folder.getParentFolder();
                }
            } catch (SQLException e) {
                log.warn("Can't build breadcrumbs", e);
            }
        }

        return breadcrumbs;
    }

    public void commandOpenFolder() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try {
            int folderId = Integer.parseInt(params.get("folderId"));
            if (folderId == 0) {
                this.currentFolder = null;
            } else {
                Folder targetFolder = getLearnweb().getGroupManager().getFolder(folderId);
                if (targetFolder == null) {
                    throw new IllegalArgumentException("Target folder does not exists.");
                }

                log(Action.opening_folder, targetFolder.getGroupId(), targetFolder.getId());
                this.currentFolder = targetFolder;
            }

            clearCaches();
        } catch (IllegalArgumentException | SQLException e) {
            addErrorMessage(e);
        }
    }

    public void commandBatchUpdateResources() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try {
            String action = params.get("action");
            ResourceUpdateBatch items = new ResourceUpdateBatch(params.get("items"));

            SelectLocationBean selectLocationBean = Beans.getInstance(SelectLocationBean.class);
            Group targetGroup = selectLocationBean.getTargetGroup();
            Folder targetFolder = selectLocationBean.getTargetFolder();

            switch (action) {
                case "copy":
                    this.copyResources(items, targetGroup, targetFolder, false);
                    break;
                case "move":
                    if (params.containsKey("destination")) {
                        JsonObject dest = JsonParser.parseString(params.get("destination")).getAsJsonObject();
                        int targetGroupId = dest.has("groupId") ? group.getId() : dest.get("groupId").getAsInt();
                        int targetFolderId = dest.has("folderId") ? 0 : dest.get("folderId").getAsInt();
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

            if (items.failed() > 0) {
                addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.cant_be_processed", items.failed());
            }
        } catch (IllegalArgumentException | IllegalAccessError e) {
            addGrowl(FacesMessage.SEVERITY_ERROR, e.getMessage());
            log.catching(e);
            // TODO still need to decide if growls can be used to handle errors
            // addErrorGrowl(e.getMessage(), e);
        } catch (JsonParseException | SQLException e) {
            addErrorMessage(e);
        }
    }

    private void copyResources(final ResourceUpdateBatch items, final Group targetGroup, final Folder targetFolder, boolean isRecursion) throws SQLException {
        if (targetGroup == null) {
            throw new IllegalArgumentException("Target group does not exist!");
        }
        if (!group.canViewResources(getUser())) {
            throw new IllegalAccessError("Not allowed to copy the resources!");
        }
        if (!targetGroup.canAddResources(getUser())) {
            throw new IllegalAccessError("Not allowed to add resources to target group!");
        }

        for (Resource resource : items.getResources()) {
            Resource newResource = resource.clone();
            newResource.setGroupId(HasId.getIdOrDefault(targetGroup, 0));
            newResource.setFolderId(HasId.getIdOrDefault(targetFolder, 0));
            newResource.setUser(getUser());
            newResource.save();
            log(Action.adding_resource, targetGroup.getId(), resource.getId());
        }

        for (Folder folder : items.getFolders()) {
            Folder newFolder = new Folder(folder);
            newFolder.setGroupId(HasId.getIdOrDefault(targetGroup, 0));
            newFolder.setParentFolderId(HasId.getIdOrDefault(targetFolder, 0));
            newFolder.setUserId(getUser().getId());
            newFolder.save();
            log(Action.add_folder, targetGroup.getId(), newFolder.getId());

            ResourceUpdateBatch copyChild = new ResourceUpdateBatch(folder.getResources(), folder.getSubFolders());
            copyResources(copyChild, targetGroup, newFolder, true);

            items.addTotalSize(copyChild.size());
            items.addTotalFailed(copyChild.failed());
        }

        if (!isRecursion && items.getTotalSize() > 0) {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.copied_successfully", items.getTotalSize());
        }
    }

    private void moveResources(ResourceUpdateBatch items, Integer targetGroupId, Integer targetFolderId) throws SQLException {
        int skipped = 0;
        if (targetGroupId == null) {
            SelectLocationBean selectLocationBean = Beans.getInstance(SelectLocationBean.class);
            if (selectLocationBean.getTargetGroup() != null) {
                targetGroupId = selectLocationBean.getTargetGroup().getId();
                targetFolderId = HasId.getIdOrDefault(selectLocationBean.getTargetFolder(), 0);
            } else {
                throw new IllegalArgumentException("Target group does not exist!");
            }
        }

        if (targetGroupId != 0) {
            Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);
            if (!targetGroup.canAddResources(getUser())) {
                throw new IllegalAccessError("You are not allowed to add resources to target group!");
            }
        }

        for (Folder folder : items.getFolders()) {
            if (isDeleteRestricted(folder)) {
                skipped++;
                continue;
            }

            folder.moveTo(targetGroupId, targetFolderId);

            log(Action.move_folder, folder.getGroupId(), folder.getId(), folder.getTitle());
        }

        for (Resource resource : items.getResources()) {
            if (isDeleteRestricted(resource)) {
                skipped++;
                continue;
            }

            resource.moveTo(targetGroupId, targetFolderId);

            log(Action.move_resource, resource.getGroupId(), resource.getId(), resource.getTitle());
        }

        if (skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        }
        if (items.size() - skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.moved_successfully", items.size() - skipped);
            clearCaches();
        }
    }

    private void deleteResources(ResourceUpdateBatch items) throws SQLException {
        int skipped = 0;

        for (Folder folder : items.getFolders()) {
            if (isDeleteRestricted(folder)) {
                skipped++;
                continue;
            }

            folder.delete();
            log(Action.deleting_folder, folder.getGroupId(), folder.getId(), folder.getTitle());

            if (folder.equals(currentFolder)) {
                currentFolder = null;
            }
        }

        for (Resource resource : items.getResources()) {
            if (isDeleteRestricted(resource)) {
                skipped++;
                continue;
            }

            resource.delete();
            log(Action.deleting_resource, resource.getGroupId(), resource.getId(), resource.getTitle());
        }

        if (items.size() - skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.deleted_successfully", items.size() - skipped);
            clearCaches();
        }

        if (skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        }
    }

    private void tagResources(ResourceUpdateBatch items, String tag) throws SQLException {
        int skipped = 0;
        for (Resource resource : items.getResources()) {
            if (!resource.canAnnotateResource(getUser())) {
                addGrowl(FacesMessage.SEVERITY_ERROR, "Sorry, you don't have permissions to annotate this resource '{0}'.", resource.getTitle());
                skipped++;
                continue;
            }

            resource.addTag(tag, getUser());
            log(Action.tagging_resource, resource.getGroupId(), resource.getId(), tag);
        }

        if (!items.getResources().isEmpty()) {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.annotated_successfully", items.getResources().size());
        }

        if (skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", skipped);
        }
    }

    private boolean isDeleteRestricted(AbstractResource resource) throws SQLException {
        if (!resource.isEditPossible()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "group_resources.denied_locked", resource.getTitle());
            return true;
        }

        if (!resource.canDeleteResource(getUser())) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "Sorry, you don't have permissions to delete this resource '{0}'.", resource.getTitle());
            return true;
        }

        return false;
    }

    /* ------------------------ Properties getters/setters ------------------------ */

    public Group getGroup() {
        return group;
    }

    public Folder getCurrentFolder() {
        return currentFolder;
    }

    public TreeNode getSelectedTreeNode() {
        return selectedTreeNode;
    }

    public void setSelectedTreeNode(TreeNode selectedTreeNode) {
        this.selectedTreeNode = selectedTreeNode;
    }

    public ResourceView getView() {
        return view;
    }

    public void setView(final ResourceView view) {
        this.view = view;
    }

    public boolean isShowFoldersTree() {
        return showFoldersTree;
    }

    public void setShowFoldersTree(final boolean showFoldersTree) {
        this.showFoldersTree = showFoldersTree;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        if (StringUtils.isBlank(searchQuery)) {
            this.searchQuery = null;
        } else if (!searchQuery.equalsIgnoreCase(this.searchQuery)) {
            this.searchQuery = searchQuery;
            log(Action.group_resource_search, group.getId(), 0, searchQuery);
        }
    }
}
