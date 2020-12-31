package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.PrivateGroup;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class SelectLocationBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6699391944318695838L;
    //private static final Logger log = LogManager.getLogger(SelectLocationBean.class);

    private Group privateGroup;

    private Group targetGroup;
    private Folder targetFolder;

    private TreeNode targetNode;
    private transient DefaultTreeNode groupsTree;
    private Instant groupsTreeUpdate;

    @PostConstruct
    public void init() {
        //log.debug("Create new SelectLocationBean");

        privateGroup = new PrivateGroup(getLocaleMessage("myPrivateResources"), getUser());
    }

    public Group getTargetGroup() {
        if (targetGroup == null && targetFolder != null) {
            targetGroup = targetFolder.getGroup();
        }

        return targetGroup;
    }

    public void setTargetGroup(final Group targetGroup) {
        this.targetGroup = targetGroup;
    }

    public Folder getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(final Folder targetFolder) {
        this.targetFolder = targetFolder;
    }

    public TreeNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(TreeNode targetNode) {
        this.targetNode = targetNode;
    }

    public void onTargetNodeSelect(NodeSelectEvent event) {
        String type = event.getTreeNode().getType();

        if ("group".equals(type)) {
            targetGroup = (Group) event.getTreeNode().getData();
            targetFolder = null;
        } else if ("folder".equals(type)) {
            targetGroup = null;
            targetFolder = (Folder) event.getTreeNode().getData();
        }
    }

    public TreeNode getGroupsAndFoldersTree() {
        User user = getUser();
        if (user == null) {
            return null;
        }

        if (groupsTree == null || groupsTreeUpdate.isBefore(Instant.now().minus(Duration.ofSeconds(30)))) {
            DefaultTreeNode treeNode = new DefaultTreeNode("WriteAbleGroups");

            TreeNode privateGroupNode = new DefaultTreeNode("group", privateGroup, treeNode);
            targetGroup = privateGroup;
            privateGroupNode.setSelected(true);

            for (Group group : user.getWriteAbleGroups()) {
                TreeNode groupNode = new DefaultTreeNode("group", group, treeNode);
                Group.getChildNodesRecursively(groupNode, group, 0);
            }

            groupsTree = treeNode;
            groupsTreeUpdate = Instant.now();
        }

        return groupsTree;
    }
}
