package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Submission;
import de.l3s.learnweb.SubmissionManager.SubmittedResources;
import de.l3s.util.StringHelper;

/**
 * @author Philipp
 */
@ManagedBean
@ViewScoped
public class SubmissionModeratorBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2494182373382483709L;
    private final static Logger log = Logger.getLogger(SubmissionModeratorBean.class);

    private int submissionId = -1;

    private Submission submission;
    private SubmittedResources selectedUserSubmission;

    @ManagedProperty(value = "#{rightPaneBean}")
    private RightPaneBean rightPaneBean;

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
            addFatalMessage(e);
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

    // methods required to show resources in right panel
    public void actionSelectGroupItem()
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
                    rightPaneBean.setViewResource(resource);
                }
                else
                    throw new NullPointerException("Target resource does not exists");
            }

        }
        catch(NullPointerException | SQLException e)
        {
            log.error(e);
        }
    }

    public RightPaneBean getRightPaneBean()
    {
        return rightPaneBean;
    }

    public void setRightPaneBean(RightPaneBean rightPaneBean)
    {
        this.rightPaneBean = rightPaneBean;
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
