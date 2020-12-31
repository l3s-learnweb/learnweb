package de.l3s.learnweb.resource.submission;

import java.io.Serializable;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

    private int submissionId;

    private Submission submission;
    private SubmittedResources selectedUserSubmission;

    @Inject
    private SubmissionDao submissionDao;

    public void onLoad() {
        submission = submissionDao.findById(submissionId).orElseThrow(BeanAssert.NOT_FOUND);
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
            submissionDao.insertSubmissionStatus(submission.getId(), userId, false);
        }
    }

    public void lockSubmission(int userId) {
        if (selectedUserSubmission != null) {
            selectedUserSubmission.setSubmitStatus(true);
            submissionDao.insertSubmissionStatus(submission.getId(), userId, true);
        }
    }

}
