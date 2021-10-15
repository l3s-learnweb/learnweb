package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.TreeNode;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Comment;
import de.l3s.learnweb.resource.CommentDao;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class AdminTextAnalysisBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -3957625443067966969L;

    private String textBR;
    private String textNL;
    private int usersCount = 0;
    private int commentCount = 0;
    private List<Comment> comments;
    private TreeNode treeRoot;
    private TreeNode[] selectedNodes;

    @Inject
    private CommentDao commentDao;

    @PostConstruct
    public void init() {
        treeRoot = BeanHelper.createGroupsUsersTree(getUser(), getLocale(), true);
    }

    public void onAnalyseComments() {
        Collection<Integer> selectedUsers = BeanHelper.getSelectedUsers(selectedNodes);
        usersCount = selectedUsers.size();

        if (selectedUsers.isEmpty()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "You have to select at least one user.");
            return;
        }

        comments = commentDao.findByUserIds(selectedUsers);

        commentCount = comments.size();

        StringBuilder sbNL = new StringBuilder();
        StringBuilder sbBR = new StringBuilder();
        for (Comment comment : comments) {
            sbNL.append(comment.getText());
            sbNL.append("\n");
            //sbNL.append("\n-----\n");

            sbBR.append(comment.getText());
            sbBR.append("<br/>");
        }

        textNL = sbNL.toString();
        textBR = sbBR.toString();
    }

    public String getTextBR() {
        return textBR;
    }

    public String getTextNL() {
        return textNL;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public TreeNode getTreeRoot() {
        return treeRoot;
    }

    public TreeNode[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(final TreeNode[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public List<Comment> getComments() {
        return comments;
    }
}
