package de.l3s.learnweb.resource.submission;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.submission.SubmissionManager.SubmittedResources;

/**
 * @author Philipp
 */
@Named
@ViewScoped
public class SubmissionModeratorBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2494182373382483709L;
    private static final Logger log = Logger.getLogger(SubmissionModeratorBean.class);

    private int submissionId = -1;

    private Submission submission;
    private SubmittedResources selectedUserSubmission;

    public void onLoad()
    {
        try
        {
            submission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);
            if(null == submission)
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "missing parameter");
            }
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }

    }

    public int getSubmissionId()
    {
        return submissionId;
    }

    public void setSubmissionId(int submissionId)
    {
        this.submissionId = submissionId;
    }

    public Submission getSubmission()
    {
        return submission;
    }

    public SubmittedResources getSelectedUserSubmission()
    {
        return selectedUserSubmission;
    }

    public void setSelectedUserSubmission(SubmittedResources selectedUserSubmission)
    {
        this.selectedUserSubmission = selectedUserSubmission;
    }

    public void unlockSubmission(int userId)
    {
        if(selectedUserSubmission != null)
        {
            selectedUserSubmission.setSubmitStatus(false);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(submission.getId(), userId, false);
        }
    }

    public void lockSubmission(int userId)
    {
        if(selectedUserSubmission != null)
        {
            selectedUserSubmission.setSubmitStatus(true);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(submission.getId(), userId, true);
        }
    }

}
