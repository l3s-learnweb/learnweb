package de.l3s.learnweb.group;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.primefaces.model.TreeNode;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.AbstractResource;
import de.l3s.learnweb.resource.Folder;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
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
    private static final int PAGE_SIZE = 48;

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

    // In group search/filters
    private String searchQuery;
    private final SearchFilters searchFilters = new SearchFilters(SearchMode.group);

    // resources
    private transient AbstractPaginator paginator;
    private transient List<Folder> folders;
    private transient List<Folder> breadcrumbs;
    private transient TreeNode foldersTree;
    private TreeNode selectedTreeNode; // Selected node in the left Folder's panel

    @Inject
    private GroupDao groupDao;

    @Inject
    private FolderDao folderDao;

    @Inject
    private ResourceDao resourceDao;

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

    public void onLoad() {
        User user = getUser();
        BeanAssert.authorized(user);

        if (user.getOrganisation().getId() == 480) {
            view = ResourceView.list;
        }

        if (groupId > 0) {
            group = groupDao.findById(groupId);
            BeanAssert.isFound(group);

            BeanAssert.hasPermission(group.canViewResources(getUser()));
            group.setLastVisit(user);
        } else {
            group = new PrivateGroup(getLocaleMessage("myPrivateResources"), getUser());
        }

        if (folderId > 0) {
            currentFolder = folderDao.findById(folderId);
            BeanAssert.validate(currentFolder, "The requested folder can't be found.");
        }
    }

    public boolean isMember() {
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
            } catch (IOException | SolrServerException e) {
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

    private SolrPaginator getResourcesFromSolr(int groupId, int folderId, String query, User user) throws IOException, SolrServerException {
        SolrSearch solrSearch = new SolrSearch(StringUtils.isEmpty(query) ? "*" : query, user);
        solrSearch.setFilterGroups(groupId);
        solrSearch.setFilterFolder(folderId, !StringUtils.isEmpty(query));
        solrSearch.setResultsPerPage(PAGE_SIZE);
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

        // TODO @astappiev: these filters are specific for archiveweb which doesn't exist any more. Couldn't it be generalized for the fields defined per organisation?
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

    public List<Folder> getSubFolders() {
        if (folders == null) {
            folders = currentFolder == null ? group.getSubFolders() : currentFolder.getSubFolders();
        }
        return folders;
    }

    public TreeNode getFoldersTree() {
        if (foldersTree == null) {
            foldersTree = Group.getFoldersTree(group, HasId.getIdOrDefault(currentFolder, 0));
        }
        return foldersTree;
    }

    public List<Folder> getBreadcrumbs() {
        if (breadcrumbs == null && currentFolder != null) {
            breadcrumbs = new ArrayList<>();
            Folder folder = currentFolder;
            while (folder != null && folder.getId() > 0) {
                breadcrumbs.add(0, folder);
                folder = folder.getParentFolder();
            }
        }

        return breadcrumbs;
    }

    public void commandOpenFolder() {
        try {
            Map<String, String> params = Faces.getRequestParameterMap();
            int folderId = Integer.parseInt(params.get("folderId"));

            if (folderId == 0) {
                this.currentFolder = null;
            } else {
                Folder targetFolder = folderDao.findById(folderId);
                if (targetFolder == null) {
                    throw new IllegalArgumentException("Target folder does not exists.");
                }

                log(Action.opening_folder, targetFolder.getGroupId(), targetFolder.getId());
                this.currentFolder = targetFolder;
            }

            clearCaches();
        } catch (IllegalArgumentException e) {
            addErrorMessage(e);
        }
    }

    public void commandBatchUpdateResources() {
        try {
            Map<String, String> params = Faces.getRequestParameterMap();
            String action = params.get("action");
            ResourceUpdateBatch items = new ResourceUpdateBatch(params.get("items"), folderDao, resourceDao);

            switch (action) {
                case "copy":
                    SelectLocationBean selectLocationBean = Beans.getInstance(SelectLocationBean.class);
                    Group targetGroup = selectLocationBean.getTargetGroup();
                    Folder targetFolder = selectLocationBean.getTargetFolder();
                    this.copyResources(items, targetGroup, targetFolder, false);
                    break;
                case "move":
                    if (params.containsKey("destination")) {
                        JsonObject dest = JsonParser.parseString(params.get("destination")).getAsJsonObject();
                        int targetGroupId = dest.has("groupId") && dest.get("groupId").isJsonPrimitive() ? dest.get("groupId").getAsInt() : group.getId();
                        int targetFolderId = dest.has("folderId") && dest.get("folderId").isJsonPrimitive() ? dest.get("folderId").getAsInt() : 0;
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
                    log.error("Unsupported action: {}", action);
                    break;
            }

            if (items.failed() > 0) {
                addGrowl(FacesMessage.SEVERITY_WARN, "For some reason, {0, choice, 1#{0} resource|1<{0} of resources} can not be processed.", items.failed());
            }
        } catch (IllegalArgumentException | IllegalAccessError | JsonParseException e) { // these exceptions will have user friendly messages
            addErrorMessage(e.getMessage(), e);
        }
    }

    private void copyResources(final ResourceUpdateBatch items, final Group targetGroup, final Folder targetFolder, boolean isRecursion) {
        if (targetGroup == null) {
            throw new IllegalArgumentException("Target group does not exist!");
        }
        if (!group.canViewResources(getUser())) {
            throw new IllegalAccessError("Not allowed to copy the resources!");
        }
        if (!targetGroup.canAddResources(getUser())) {
            throw new IllegalAccessError("Not allowed to add resources to target group!");
        }

        int targetGroupId = HasId.getIdOrDefault(targetGroup, 0);
        int targetFolderId = HasId.getIdOrDefault(targetFolder, 0);

        for (Resource resource : items.getResources()) {
            dao().getResourceDao().copy(resource, targetGroupId, targetFolderId, getUser());
            log(Action.adding_resource, targetGroup.getId(), resource.getId());
        }

        for (Folder folder : items.getFolders()) {
            Folder newFolder = new Folder(folder);
            newFolder.setGroupId(targetGroupId);
            newFolder.setParentFolderId(targetFolderId);
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

    private void moveResources(ResourceUpdateBatch items, Integer targetGroupId, Integer targetFolderId) {
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
            Group targetGroup = groupDao.findById(targetGroupId);
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
            addGrowl(FacesMessage.SEVERITY_WARN, "For some reasons, {0, choice, 1#{0} resource|1<{0} of resources} were skipped.", skipped);
        }
        if (items.size() - skipped > 0) {
            addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.moved_successfully", items.size() - skipped);
            clearCaches();
        }
    }

    private void deleteResources(ResourceUpdateBatch items) {
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
            addGrowl(FacesMessage.SEVERITY_WARN, "For some reasons, {0, choice, 1#{0} resource|1<{0} of resources} were skipped.", skipped);
        }
    }

    private void tagResources(ResourceUpdateBatch items, String tag) {
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
            addGrowl(FacesMessage.SEVERITY_WARN, "For some reasons, {0, choice, 1#{0} resource|1<{0} of resources} were skipped.", skipped);
        }
    }

    private boolean isDeleteRestricted(AbstractResource resource) {
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
