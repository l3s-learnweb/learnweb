package de.l3s.learnweb.resource.submission;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

/**
 * @author Philipp Kemkes
 */
@Named
@ViewScoped
public class SubmissionModeratorBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -2494182373382483709L;
    //private static final Logger log = LogManager.getLogger(SubmissionModeratorBean.class);

    private int submissionId = -1;

    private Submission submission;
    private SubmittedResources selectedUserSubmission;

    public void onLoad() throws SQLException {
        submission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);
        BeanAssert.validateNotNull(submission);
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }

    public Submission getSubmission() {
        return submission;
    }

    public SubmittedResources getSelectedUserSubmission() {
        return selectedUserSubmission;
    }

    public void setSelectedUserSubmission(SubmittedResources selectedUserSubmission) {
        this.selectedUserSubmission = selectedUserSubmission;
    }

    public void unlockSubmission(int userId) {
        if (selectedUserSubmission != null) {
            selectedUserSubmission.setSubmitStatus(false);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(submission.getId(), userId, false);
        }
    }

    public void lockSubmission(int userId) {
        if (selectedUserSubmission != null) {
            selectedUserSubmission.setSubmitStatus(true);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(submission.getId(), userId, true);
        }
    }

}
