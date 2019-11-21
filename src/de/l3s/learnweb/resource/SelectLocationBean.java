package de.l3s.learnweb.resource;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.user.User;
import org.apache.log4j.Logger;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;

@Named
@ViewScoped
public class SelectLocationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6699391944318695838L;
    private static final Logger log = Logger.getLogger(SelectLocationBean.class);

    private static final long TREE_CACHE_MS = 15_000L;

    private Group targetGroup;
    private Folder targetFolder;

    private transient TreeNode targetNode;

    private transient long groupsTreeTime = 0L;
    private transient long foldersTreeTime = 0L;
    private transient DefaultTreeNode groupsTree;
    private transient DefaultTreeNode foldersTree;

    public Group getTargetGroup()
    {
        if (targetGroup == null && targetFolder != null)
        {
            try
            {
                return targetFolder.getGroup();
            }
            catch(SQLException e)
            {
                log.error("Can't find group by folder's group_id", e);
            }
        }

        return targetGroup;
    }

    public void setTargetGroup(final Group targetGroup)
    {
        this.targetGroup = targetGroup;
    }

    public Folder getTargetFolder()
    {
        return targetFolder;
    }

    public void setTargetFolder(final Folder targetFolder)
    {
        this.targetFolder = targetFolder;
    }

    public TreeNode getTargetNode()
    {
        return targetNode;
    }

    public void setTargetNode(TreeNode targetNode)
    {
        this.targetNode = targetNode;
    }

    public void onTargetNodeSelect(NodeSelectEvent event)
    {
        String type = event.getTreeNode().getType();

        if("group".equals(type))
        {
            targetGroup = (Group) event.getTreeNode().getData();
        }
        else if("folder".equals(type))
        {
            targetFolder = (Folder) event.getTreeNode().getData();
        }
    }

    public TreeNode getGroupsAndFoldersTree() throws SQLException
    {
        User user = getUser();
        if (user == null)
            return null;

        if(groupsTree == null || groupsTreeTime < (System.currentTimeMillis() - TREE_CACHE_MS))
        {
            groupsTree = new DefaultTreeNode("GroupsAndFoldersTree");

            TreeNode myResourcesNode = new DefaultTreeNode("group", new Group(0, UtilBean.getLocaleMessage("myPrivateResources")), groupsTree);
            myResourcesNode.setSelected(true);

            for(Group group : user.getWriteAbleGroups())
            {
                TreeNode groupNode = new DefaultTreeNode("group", group, groupsTree);
                GroupManager gm = Learnweb.getInstance().getGroupManager();
                gm.getChildNodesRecursively(group.getId(), 0, groupNode, 0);
            }
            groupsTreeTime = System.currentTimeMillis();
        }

        return groupsTree;
    }

    public TreeNode getFoldersTree() throws SQLException
    {
        User user = getUser();
        if (user == null || targetGroup == null)
            return null;

        if(foldersTree == null || foldersTreeTime < (System.currentTimeMillis() - TREE_CACHE_MS))
        {
            foldersTree = new DefaultTreeNode("FoldersTree");

            TreeNode rootFolder = new DefaultTreeNode("folder", new Folder(0, targetGroup.getId(), targetGroup.getTitle()), foldersTree);
            if (targetFolder == null) rootFolder.setSelected(true);
            rootFolder.setExpanded(true);

            GroupManager gm = Learnweb.getInstance().getGroupManager();
            gm.getChildNodesRecursively(targetGroup.getId(), 0, rootFolder, targetFolder != null ? targetFolder.getId() : 0);

            foldersTreeTime = System.currentTimeMillis();
        }

        return foldersTree;
    }
}
