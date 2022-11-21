package de.l3s.learnweb.resource.ted;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.primefaces.model.TreeNode;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class TedTranscriptLogBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1803725556672379697L;

    //private int selectedCourseId;
    private TreeNode treeRoot;
    private TreeNode[] selectedNodes;
    private Collection<Integer> selectedUsers = new TreeSet<>();

    private transient List<SimpleTranscriptLog> simpleTranscriptLogs;
    private transient List<TranscriptLog> detailedTranscriptLogs;
    private transient List<TranscriptSummary> transcriptSummaries;

    @Inject
    private TedTranscriptDao tedTranscriptDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        treeRoot = BeanHelper.createGroupsUsersTree(getUser(), true);
    }

    /**
     * Returns detailed transcript logs for selected users.
     */
    public List<TranscriptLog> getTranscriptLogs() {
        if (detailedTranscriptLogs == null) {
            detailedTranscriptLogs = tedTranscriptDao.findTranscriptLogsByUserIds(selectedUsers);
        }
        return detailedTranscriptLogs;
    }

    public void onSubmitSelectedUsers() {
        this.selectedUsers = BeanHelper.getSelectedUsers(selectedNodes);

        resetTranscriptLogs();
        resetTranscriptSummaries();
    }

    /**
     * Returns transcript logs of selected users aggregating selection, deselection and user annotation counts.
     */
    public List<SimpleTranscriptLog> getSimpleTranscriptLogs() {
        if (simpleTranscriptLogs == null) {
            if (selectedUsers.isEmpty()) {
                return new ArrayList<>();
            }

            simpleTranscriptLogs = tedTranscriptDao.findSimpleTranscriptLogs(selectedUsers);
        }
        return simpleTranscriptLogs;
    }

    /**
     * Returns transcript summaries of selected users.
     */
    public List<TranscriptSummary> getTranscriptSummaries() {
        if (transcriptSummaries == null) {
            if (selectedUsers.isEmpty()) {
                return new ArrayList<>();
            }

            transcriptSummaries = tedTranscriptDao.findTranscriptSummariesByUserIds(selectedUsers);
        }
        return transcriptSummaries;
    }

    public void resetTranscriptLogs() {
        simpleTranscriptLogs = null;
        detailedTranscriptLogs = null;
    }

    public void resetTranscriptSummaries() {
        transcriptSummaries = null;
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
}
