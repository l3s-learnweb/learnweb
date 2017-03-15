package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Group;
import de.l3s.learnweb.GroupItem;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnwebBeans.GroupDetailBean.RPAction;
import de.l3s.util.StringHelper;

@ManagedBean
@ViewScoped
public class MyResourcesBean extends ApplicationBean implements Serializable
{
    private final static long serialVersionUID = 5680533799976460331L;
    private final static Logger log = Logger.getLogger(MyResourcesBean.class);

    private List<Resource> resources;

    private RPAction rightPanelAction = null;
    private Boolean reloadLogs = false;

    private Resource clickedResource;
    private Folder currentFolder;
    private Folder selectedFolder;
    private GroupItem clickedGroupItem; // Preview of resource/folder

    private boolean rootFolder = true;
    private List<Folder> breadcrumb;
    private boolean folderView = false;

    // Folders tree
    private TreeNode selectedTargetNode;
    private int selectedResourceTargetGroupId;
    private int selectedResourceTargetFolderId;

    private int groupId;

    @ManagedProperty(value = "#{resourceDetailBean}")
    private ResourceDetailBean resourceDetailBean;

    @ManagedProperty(value = "#{addResourceBean}")
    private AddResourceBean addResourceBean;

    public MyResourcesBean() throws SQLException
    {
        if(getUser() == null) // not logged in
            return;

        if(getParameterInt("resource_id") != null)
            setRightPanelAction("viewResource");

        if(getParameter("save_url") != null)
            setRightPanelAction("newResource");

        breadcrumb = new ArrayList<Folder>();
        clickedResource = new Resource();
    }

    public void preRenderView(ComponentSystemEvent e) throws SQLException
    {
        if(getUser() == null) // not logged in
            return;

        if(isAjaxRequest())
        {
            log.debug("Skip ajax request");
            return;
        }

        if(groupId > 0)
        {
            rootFolder = false;
            Group group = getLearnweb().getGroupManager().getGroupById(groupId);
            Folder folder = new Folder(0, groupId, group.getTitle());
            breadcrumb.add(0, folder);
            updateBreadcrumb();
            resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(groupId, getSelectedFolderId(), getUser().getId(), 1000);
        }
        else if(getParameterInt("group_id") != null && getParameterInt("folder_id") != null)
        {
            if(groupId == 0 && getSelectedFolderId() == 0)
            {
                rootFolder = false;
                Folder folder = new Folder(0, 0, UtilBean.getLocaleMessage("myPrivateResources"));
                breadcrumb.add(folder);
                resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(0, 0, getUser().getId(), 1000);
            }
        }

        if(getParameterInt("resource_id") != null)
            setRightPanelAction("viewResource");

        if(getParameter("save_url") != null)
            setRightPanelAction("newResource");
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

    /*
    public boolean canDeleteTag(Object tagO) throws SQLException
    {
        if(!(tagO instanceof Tag))
            return false;
    
        User user = getUser();
        if(null == user)
            return false;
        if(user.isAdmin() || user.isModerator())
            return true;
    
        Tag tag = (Tag) tagO;
        User owner = clickedResource.getTags().getElementOwner(tag);
        if(user.equals(owner))
            return true;
        return false;
    }
    */
    public void addSelectedResourceToGroup() throws SQLException
    {
        User user = getUser();
        if(null == user)
        {
            addGrowl(FacesMessage.SEVERITY_ERROR, "loginRequiredText");
            return;
        }

        Resource resource = clickedResource;

        Group targetGroup = Learnweb.getInstance().getGroupManager().getGroupById(selectedResourceTargetGroupId);

        // add resource to a group if selected
        if(targetGroup != null && targetGroup.getId() > 0)
        {

            if(resource.getGroupId() == 0)
            {
                resource.setGroupId(targetGroup.getId());
                resource.save();
            }
            else
            {
                Resource newResource = resource.clone();
                newResource.setGroupId(targetGroup.getId());
                getUser().addResource(newResource);
            }

            user.setActiveGroup(targetGroup.getId());
            addGrowl(FacesMessage.SEVERITY_INFO, "addedResourceToGroup", resource.getTitle(), targetGroup.getTitle());
        }

        log(Action.adding_resource, selectedResourceTargetGroupId, resource.getId(), "");

    }

    public void editClickedResource() throws SQLException
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

    public List<Resource> getResources()
    {
        return resources;
    }

    public Resource getClickedResource()
    {
        if(clickedGroupItem instanceof Resource)
        {
            return (Resource) getClickedGroupItem();
        }

        return clickedResource;
    }

    public void setClickedResource(Resource clickedResource)
    {
        if(rightPanelAction != RPAction.editResource || this.clickedResource != clickedResource)
        {
            rightPanelAction = RPAction.viewResource;
            this.clickedResource = clickedResource;
        }
    }

    public Folder getClickedFolder()
    {
        if(clickedGroupItem instanceof Folder)
        {
            return (Folder) getClickedGroupItem();
        }

        return null;
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
            if(f.equals(getClickedFolder()))
                delete = true;
        }
    }

    public void setClickedFolder(Folder clickedFolder) throws SQLException
    {
        if(clickedFolder != null)
        {
            this.currentFolder = clickedFolder;
            setClickedGroupItem(clickedFolder);
            setSelectedFolder(clickedFolder);
            this.rootFolder = false;
            if(breadcrumb.contains(clickedFolder))
                deleteEntriesFromBreadcrumb();
            else
                breadcrumb.add(clickedFolder);
        }
        resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(clickedFolder.getGroupId(), clickedFolder.getId(), getUser().getId(), 1000);
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
            else if(itemType != null && itemType.equals("group") && itemId > 0)
            {
                Group group = getLearnweb().getGroupManager().getGroupById(itemId);
                if(group != null)
                {
                    Folder folder = new Folder(0, itemId, group.getTitle());
                    this.setClickedGroupItem(folder);
                }
                else
                    throw new NullPointerException("Target group does not exists");
            }
            else if(itemType != null && itemType.equals("group") && itemId == 0)
            {
                Folder folder = new Folder(0, 0, UtilBean.getLocaleMessage("myPrivateResources"));
                folder.setUserId(getUser().getId());
                this.setClickedGroupItem(folder);
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

    public void actionOpenFolder()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            int folderId = StringHelper.parseInt(params.get("itemId"));
            if(!isRootFolder() && folderId > 0)
            {
                Folder folder = getLearnweb().getGroupManager().getFolder(folderId);
                if(folder != null)
                {
                    this.setSelectedFolder(folder);
                    this.setClickedFolder(folder);
                }
                else
                    throw new NullPointerException("Target folder does not exists");
            }
            else if(isRootFolder() && folderId > 0)
            {
                Group group = getLearnweb().getGroupManager().getGroupById(folderId);
                if(group != null)
                {
                    Folder folder = new Folder(0, folderId, group.getTitle());
                    this.setClickedFolder(folder);
                    this.setSelectedFolder(folder);
                }
                else
                    throw new NullPointerException("Target group does not exists");
            }
            else if(isRootFolder() && folderId == 0)
            {
                Folder folder = new Folder(0, 0, UtilBean.getLocaleMessage("myPrivateResources"));
                folder.setUserId(getUser().getId());
                this.setClickedFolder(folder);
                this.setSelectedFolder(folder);
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
            /*case "move":
                JSONObject dest = params.containsKey("destination") ? new JSONObject(params.get("destination")) : null;
                this.moveGroupItems(items, dest);
                break;*/
            case "delete":
                this.deleteItems(items);
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

    private void addTagToGroupItems(JSONArray objects, String tag)
    {
        try
        {
            int numResources = 0, numSkipped = 0;

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
                    this.setClickedGroupItem(resource);
                    this.setRightPanelAction(RPAction.editResource);
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
            addFatalMessage(e);
        }
    }
    /*
    public boolean canDeleteResourcesInGroup(Group group) throws SQLException
    {
        return group.canDeleteResources(getUser());
    }
    
    public boolean canEditResourcesInGroup(Group group) throws SQLException
    {
        return group.canEditResources(getUser());
    }
    
    
    @Deprecated
    public boolean canDeleteResources() throws SQLException
    {
        if(getCurrentFolder() != null)
            return getCurrentFolder().getTitle().equalsIgnoreCase("my private resources"); // TODO this should be removed
        return false;
    }
    */

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
                        if(clickedGroupItem != null && clickedGroupItem.equals(resource))
                            clickedGroupItem = null;

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
                addGrowl(FacesMessage.SEVERITY_INFO, "resourcesDeletedSuccessfully", numResources);
                if(numResources > 0)
                    resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(0, 0, getUser().getId(), 1000);
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
            log.debug(e);
        }
    }

    public Boolean getReloadLogs()
    {
        return reloadLogs;
    }

    public void setReloadLogs(Boolean reloadLogs)
    {
        this.reloadLogs = reloadLogs;
    }

    public Folder getCurrentFolder()
    {
        return currentFolder;
    }

    public List<Folder> getFolders() throws SQLException
    {
        if(rootFolder)
        {
            LinkedList<Folder> folders = new LinkedList<Folder>();
            Folder myPrivateFolder = new Folder(0, 0, UtilBean.getLocaleMessage("myPrivateResources"));
            myPrivateFolder.setUserId(getUser().getId());
            folders.add(myPrivateFolder);
            folders.addAll(Learnweb.getInstance().getGroupManager().getGroupsForMyResources(getUser().getId()));
            return folders;
        }
        else if(groupId > 0)
            return Learnweb.getInstance().getGroupManager().getFolders(groupId, getSelectedFolderId());
        else if(getClickedFolder() != null)
            return Learnweb.getInstance().getGroupManager().getFolders(getClickedFolder().getGroupId(), getSelectedFolderId(), getUser().getId());

        return new ArrayList<Folder>();
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
            UtilBean.getAddResourceBean().setResourceTargetFolderId(getSelectedFolderId());
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
        if (this.getAddResourceBean() != null && this.getAddResourceBean().getResource() != null) {
            return this.getAddResourceBean().getResource().getUrl();
        }

        return null;
    }

    public void setUrlToSave(String url) throws SQLException
    {
        if(url != null && !url.isEmpty())
        {
            this.rightPanelAction = RPAction.newResource;
            this.addResourceBean.getResource().setStorageType(2);
            this.addResourceBean.getResource().setUrl(url);
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

    public boolean isFolderView()
    {
        return folderView;
    }

    public void setFolderView(boolean folderView)
    {
        this.folderView = folderView;
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

    public ResourceDetailBean getResourceDetailBean()
    {
        return resourceDetailBean;
    }

    public void setResourceDetailBean(ResourceDetailBean resourceDetailBean)
    {
        this.resourceDetailBean = resourceDetailBean;
    }

    public AddResourceBean getAddResourceBean() {
        return addResourceBean;
    }

    public void setAddResourceBean(AddResourceBean addResourceBean) {
        this.addResourceBean = addResourceBean;
    }
}
