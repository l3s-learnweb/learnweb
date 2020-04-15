package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupManager;
import de.l3s.learnweb.group.PrivateGroup;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class SelectLocationBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6699391944318695838L;
    private static final Logger log = Logger.getLogger(SelectLocationBean.class);

    private Group targetGroup;
    private Folder targetFolder;

    private transient TreeNode targetNode;
    private transient DefaultTreeNode groupsTree;
    private Instant groupsTreeUpdate;

    public SelectLocationBean()
    {
        log.debug("Create new SelectLocationBean");
    }

    public Group getTargetGroup()
    {
        if(targetGroup == null && targetFolder != null)
        {
            try
            {
                targetGroup = targetFolder.getGroup();
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
            targetFolder = null;
        }
        else if("folder".equals(type))
        {
            targetGroup = null;
            targetFolder = (Folder) event.getTreeNode().getData();
        }
    }

    public TreeNode getGroupsAndFoldersTree() throws SQLException
    {
        User user = getUser();
        if(user == null)
            return null;

        if(groupsTree == null || groupsTreeUpdate.isBefore(Instant.now().minus(Duration.ofSeconds(15))))
        {
            GroupManager gm = Learnweb.getInstance().getGroupManager();
            DefaultTreeNode treeNode = new DefaultTreeNode("WriteAbleGroups");

            Group privateGroup = new PrivateGroup(getLocaleMessage("myPrivateResources"), getUser());
            TreeNode privateGroupNode = new DefaultTreeNode("group", privateGroup, treeNode);
            privateGroupNode.setSelected(true);

            for(Group group : user.getWriteAbleGroups())
            {
                TreeNode groupNode = new DefaultTreeNode("group", group, treeNode);
                gm.getChildNodesRecursively(groupNode, group, 0);
            }

            groupsTree = treeNode;
            groupsTreeUpdate = Instant.now();
        }

        return groupsTree;
    }
}
