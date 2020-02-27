package de.l3s.learnweb.resource;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.Action;
import de.l3s.util.StringHelper;

@Named
@ViewScoped
public class MyResourcesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5680533799976460331L;
    private static final Logger log = Logger.getLogger(MyResourcesBean.class);

    // Group base attributes
    private int groupId; // Current group id
    private Group group; // Current group

    private Folder selectedFolder; // Current opened folder
    private boolean rootFolder = true;

    //Grid or List view of group resources
    private boolean gridView = true;

    private List<Resource> resources;
    private List<Folder> breadcrumb;

    // Folders tree (dialog)
    private TreeNode selectedTargetNode;
    private int selectedResourceTargetGroupId;
    private int selectedResourceTargetFolderId;

    @Inject
    private ResourceDetailBean resourceDetailBean;

    @Inject
    private AddResourceBean addResourceBean;

    @Inject
    private AddFolderBean addFolderBean;

    public MyResourcesBean()
    {
        if(getUser() == null) // not logged in
            return;

        breadcrumb = new ArrayList<>();
    }

    public void onLoad() throws SQLException
    {
        if(getUser() == null) // not logged in
            return;

        if(isAjaxRequest())
        {
            //log.debug("Skip ajax request");
            return;
        }

        if(groupId > 0)
        {
            rootFolder = false;
            group = getLearnweb().getGroupManager().getGroupById(groupId);
            Folder folder = new Folder(0, groupId, group.getTitle());
            breadcrumb.add(0, folder);
            updateBreadcrumb();
            updateResources();
        }
        else if(getParameterInt("group_id") != null && getParameterInt("folder_id") != null)
        {
            if(groupId == 0 && getSelectedFolderId() == 0)
            {
                rootFolder = false;
                Folder folder = new Folder(0, 0, getLocaleMessage("myPrivateResources"));
                breadcrumb.add(folder);
                updateResources();
            }
        }

        if(getParameter("save_url") != null)
            resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
    }

    public void updateBreadcrumb() throws SQLException
    {
        Folder folder = getSelectedFolder();
        while(folder != null)
        {
            breadcrumb.add(1, folder);
            folder = folder.getParentFolder();
        }
    }

    public void updateResources()
    {
        try
        {
            int groupId = this.groupId > 0 ? this.groupId : (selectedFolder == null || selectedFolder.getGroupId() <= 0 ? 0 : selectedFolder.getGroupId());
            int folderId = getSelectedFolderId();
            resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(groupId, folderId, getUser().getId(), 1000);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    private void deleteEntriesFromBreadcrumb()
    {
        ListIterator<Folder> iter = breadcrumb.listIterator();
        boolean delete = false;
        while(iter.hasNext())
        {
            Folder f = iter.next();
            if(delete)
            {
                iter.remove();
                continue;
            }
            if(f.equals(selectedFolder))
                delete = true;
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
                    resourceDetailBean.setViewResource(folder);
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                {
                    resourceDetailBean.setViewResource(resource);
                }
                else
                    throw new NullPointerException("Target resource does not exists");
            }
            else if(itemType != null && itemType.equals("group") && itemId > 0)
            {
                Group group = getLearnweb().getGroupManager().getGroupById(itemId);
                if(group != null)
                {
                    Folder folder = new Folder(0, itemId, group.getTitle());
                    resourceDetailBean.setViewResource(folder);
                }
                else
                    throw new NullPointerException("Target group does not exists");
            }
            else if(itemType != null && itemType.equals("group") && itemId == 0)
            {
                Folder folder = new Folder(0, 0, getLocaleMessage("myPrivateResources"));
                folder.setUserId(getUser().getId());
                resourceDetailBean.setViewResource(folder);
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

    public void actionOpenFolder()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            int folderId = StringHelper.parseInt(params.get("folderId"));
            if(!isRootFolder() && folderId > 0)
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(folderId);
                if(folder != null)
                    this.setSelectedFolder(folder);
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else if(isRootFolder() && folderId > 0)
            {
                Group group = getLearnweb().getGroupManager().getGroupById(folderId);
                if(group != null)
                {
                    Folder folder = new Folder(0, folderId, group.getTitle());
                    this.setSelectedFolder(folder);
                }
                else
                    throw new NullPointerException("Target group does not exists");
            }
            else if(isRootFolder() && folderId == 0)
            {
                Folder folder = new Folder(0, 0, getLocaleMessage("myPrivateResources"));
                folder.setUserId(getUser().getId());
                this.setSelectedFolder(folder);
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
                this.deleteItems(items);
                break;
            case "add-tag":
                String tag = params.get("tag");
                this.addTagToGroupItems(items, tag);
                break;
            default:
                log.error("Unsupported action: " + action);
                break;
            }

            updateResources();
        }
        catch(JSONException e)
        {
            addErrorMessage(e);
        }
    }

    private void addTagToGroupItems(JSONArray objects, String tag)
    {
        try
        {
            int numResources = 0, numSkipped = 0;

            // TODO Dupe: same as in GroupResourcesBean
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
                addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.annotated_successfully", numResources);
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
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

            if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                {
                    resourceDetailBean.setEditResource(resource);
                }
                else
                {
                    addGrowl(FacesMessage.SEVERITY_ERROR, "Target resource doesn't exists");
                }
            }
            else
            {
                throw new NullPointerException("Unsupported itemType");
            }
        }
        catch(NullPointerException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    private void actionCopyGroupItems(JSONArray objects)
    {
        try
        {
            int numFolders = 0, numResources = 0, numSkipped = 0, targetGroupId = selectedResourceTargetGroupId, targetFolderId = selectedResourceTargetFolderId;

            Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(targetGroupId);

            if(targetGroupId != 0 && !targetGroup.canAddResources(getUser()))
            {
                addGrowl(FacesMessage.SEVERITY_ERROR, "You are not allowed to add new resources in target group");
                return;
            }

            // TODO Dupe: same as in GroupResourcesBean
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
                addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.copied_successfully", numFolders + numResources);
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", numSkipped);
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
                int itemId = item.getInt("itemId");
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
                addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.moved_successfully", numFolders + numResources);
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    private void deleteItems(JSONArray objects)
    {
        try
        {
            int numResources = 0, numSkipped = 0;

            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);

                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"));

                if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null)
                    {
                        //int resourceGroupId = resource.getGroupId();
                        String resourceTitle = resource.getTitle();
                        if(resourceDetailBean.isTheResourceClicked(resource))
                            resourceDetailBean.resetPane();

                        getUser().deleteResource(resource);
                        numResources++;

                        log(Action.deleting_resource, 0, itemId, resourceTitle);
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

            if(numResources > 0)
            {
                addGrowl(FacesMessage.SEVERITY_INFO, "group_resources.deleted_successfully", numResources);
                updateResources();
                resourceDetailBean.resetPane();
            }

            if(numSkipped > 0)
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "group_resources.skipped", numSkipped);
            }
        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            addErrorMessage(e);
        }
    }

    public void actionCreateItem() throws IllegalAccessException, InvocationTargetException, IOException
    {
        String type = getParameter("type");

        // Set target group and folder in beans
        switch(type)
        {
            case "folder":
                addFolderBean.reset();
                addFolderBean.setTarget(group, selectedFolder);
                break;
            default:
                addResourceBean.reset();
                addResourceBean.setTarget(group, selectedFolder);
                addResourceBean.getResource().setStorageType(Resource.LEARNWEB_RESOURCE);
                break;
        }

        // Set target view and defaults
        switch(type)
        {
            case "folder":
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newFolder);
                break;
            case "file":
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
                addResourceBean.getResource().setType(ResourceType.file);
                break;
            case "url":
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
                addResourceBean.getResource().setType(ResourceType.website);
                addResourceBean.getResource().setStorageType(Resource.WEB_RESOURCE);
                break;
            case "glossary2":
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
                addResourceBean.setResourceTypeGlossary();
                break;
            case "survey":
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
                addResourceBean.getResource().setType(ResourceType.survey);
                break;
            case "newFile":
                ResourceType docType = ResourceType.parse(getParameter("docType"));
                resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newFile);
                addResourceBean.getResource().setType(docType);
                break;
            default:
                log.warn("Unsupported item type: " + type);
                break;
        }
    }

    public List<Resource> getResources()
    {
        return resources;
    }

    public List<Folder> getFolders() throws SQLException
    {
        if(rootFolder)
        {
            LinkedList<Folder> folders = new LinkedList<>();
            Folder myPrivateFolder = new Folder(0, 0, getLocaleMessage("myPrivateResources"));
            myPrivateFolder.setUserId(getUser().getId());
            folders.add(myPrivateFolder);
            folders.addAll(Learnweb.getInstance().getGroupManager().getGroupsForMyResources(getUser().getId()));
            return folders;
        }
        else if(groupId > 0)
            return Learnweb.getInstance().getGroupManager().getFolders(groupId, getSelectedFolderId());
        else if(selectedFolder != null)
            return Learnweb.getInstance().getGroupManager().getFolders(selectedFolder.getGroupId(), getSelectedFolderId(), getUser().getId());

        return new ArrayList<>();
    }

    public Folder getSelectedFolder()
    {
        return selectedFolder;
    }

    public void setSelectedFolder(Folder folder) throws SQLException
    {
        if(folder != null)
        {
            rootFolder = false;
            if(breadcrumb.contains(folder))
                deleteEntriesFromBreadcrumb();
            else
                breadcrumb.add(folder);

            selectedFolder = folder;
            addResourceBean.setTarget(group, selectedFolder);

            updateResources();
        }
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

    public String getUrlToSave()
    {
        if(addResourceBean != null && addResourceBean.getResource() != null)
        {
            return addResourceBean.getResource().getUrl();
        }

        return null;
    }

    public void setUrlToSave(String url)
    {
        if(url != null && !url.isEmpty())
        {
            if(addResourceBean.getFormStep() != 1) // necessary to avoid a conflict with uploaded resources
                return;

            resourceDetailBean.setPaneAction(ResourceDetailBean.ViewAction.newResource);
            addResourceBean.getResource().setStorageType(Resource.WEB_RESOURCE);
            addResourceBean.getResource().setUrl(url);
        }
    }

    public List<Folder> getBreadcrumb()
    {
        return breadcrumb;
    }

    public boolean isRootFolder()
    {
        return rootFolder;
    }

    public void setRootFolder(boolean rootFolder)
    {
        this.rootFolder = rootFolder;
    }

    public int getGroupId()
    {
        return groupId;
    }

    public void setGroupId(int groupId)
    {
        this.groupId = groupId;
    }

    public void onTargetNodeSelect(NodeSelectEvent event)
    {
        String type = event.getTreeNode().getType();

        // TODO Dupe: same as GroupResourcesBean and SearchBean
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

    public boolean isGridView()
    {
        return gridView;
    }

    public void setGridView(boolean gridView)
    {
        this.gridView = gridView;
    }

    public TreeNode getSelectedTargetNode()
    {
        return selectedTargetNode;
    }

    public void setSelectedTargetNode(TreeNode selectedTargetNode)
    {
        this.selectedTargetNode = selectedTargetNode;
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
}
