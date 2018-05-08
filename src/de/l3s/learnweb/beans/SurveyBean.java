package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.PeerAssessmentManager.PeerAssesmentPair;
import de.l3s.learnweb.SurveyResource;
import de.l3s.learnweb.SurveyUserAnswers;
import de.l3s.learnweb.User;
import de.l3s.util.BeanHelper;

@ViewScoped
@ManagedBean
public class SurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = Logger.getLogger(SurveyBean.class);
    private int surveyResourceId;
    private int surveyUserId; // the user whose answers are viewed, by default the currently logged in user

    private boolean formEnabled;

    private SurveyResource resource;
    private SurveyUserAnswers userAnswers;

    public void onLoad()
    {
        //log.debug("survey onLoad resource_id: " + surveyResourceId + "; user_id: " + surveyUserId);

        User user = getUser();
        if(user == null)
            return;

        try
        {
            resource = (SurveyResource) getLearnweb().getResourceManager().getResource(surveyResourceId);

            if(resource == null)
            {
                addInvalidParameterMessage("resource_id");
                return;
            }

            if(!resource.canViewResource(user))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "group_resources_access_denied");
                resource = null;
                return;
            }

            // whose answers shall be viewed
            if(surveyUserId <= 0 || surveyUserId == getUser().getId()) // by default view the answers of the current user
            {
                surveyUserId = getUser().getId();
            }
            // if a user wants to see the answers of another user make sure he is a moderator or the survey is part of a peer assessment
            else if(!resource.canModerateResource(getUser()) && !canViewPeerAssessmentResult())
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to view the answers of the given user");
                log.error("Illegal access: " + BeanHelper.getRequestSummary());
                resource = null;
                return;
            }

            userAnswers = resource.getAnswersOfUser(surveyUserId);

            formEnabled = !userAnswers.isSubmitted() && surveyUserId == getUser().getId() && isValidSubmissionDate(resource);
        }
        catch(Exception e)
        {
            surveyResourceId = -1;
            addFatalMessage("Couldn't load survey; resource: ", e);
        }

    }

    private boolean canViewPeerAssessmentResult() throws SQLException
    {
        int userId = getUser().getId();
        PeerAssesmentPair pair = getLearnweb().getPeerAssessmentManager().getPair(surveyResourceId, surveyUserId, userId);

        if(pair != null)
            return true;

        log.debug("canViewPeerAssessmentResult is false");

        return false;
    }

    /**
     * This method is only called by the survey/survey.jsf page to load necessary warnings
     */
    public void onLoadEdit()
    {
        if(null == resource) // access violation detected in onLoad()
            return;

        if(isSubmitted())
            addMessage(FacesMessage.SEVERITY_ERROR, "survey_already_submitted");
        else if(!isValidSubmissionDate(resource))
            addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_error_between", resource.getStart(), resource.getEnd());
        else if(userAnswers.isSaved())
        {
            if(resource.getEnd() != null)
                addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit_until", resource.getEnd());
            else
                addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit");
        }
    }

    public boolean isSubmitted()
    {
        if(null == userAnswers)
        {
            log.warn("userAnswers is null");
            return false;
        }
        return userAnswers.isSubmitted();
    }

    /**
     *
     * @param submit true if final submit
     * @return false on error
     */
    private boolean onSaveOrSubmit(boolean submit)
    {
        if(isSubmitted())
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "This Survey has already been submitted");
            log.error("Survey already submitted. Should not happen. User: " + surveyUserId + "; Survey: " + surveyResourceId);
            return false;
        }

        try
        {
            getLearnweb().getSurveyManager().saveAnswers(userAnswers, submit);
            return true;
        }
        catch(SQLException e)
        {
            addFatalMessage("Can't save answers for User: " + surveyUserId + " for survey: " + surveyResourceId, e);
        }
        return false;
    }

    public void onSubmit()
    {
        if(onSaveOrSubmit(true))
        {
            addMessage(FacesMessage.SEVERITY_INFO, "survey_submitted");
            log(Action.survey_submit, resource.getGroupId(), surveyResourceId);
            formEnabled = false;
        }
    }

    public void onSave()
    {
        if(onSaveOrSubmit(false))
        {
            addMessage(FacesMessage.SEVERITY_INFO, "survey_saved");
            log(Action.survey_save, resource.getGroupId(), surveyResourceId);
        }
    }

    private static boolean isValidSubmissionDate(SurveyResource surveyResource)
    {
        Long currentDate = new Date().getTime();

        if(surveyResource.getStart() != null && surveyResource.getStart().getTime() > currentDate)
            return false;
        if(surveyResource.getEnd() != null && surveyResource.getEnd().getTime() < currentDate)
            return false;
        return true;
    }

    public int getSurveyResourceId()
    {
        return surveyResourceId;
    }

    public void setSurveyResourceId(int surveyResourceId)
    {
        this.surveyResourceId = surveyResourceId;
    }

    public int getSurveyUserId()
    {
        return surveyUserId;
    }

    public void setSurveyUserId(int surveyUserId)
    {
        this.surveyUserId = surveyUserId;
    }

    public SurveyResource getResource()
    {
        return resource;
    }

    public SurveyUserAnswers getUserAnswers()
    {
        return userAnswers;
    }

    public boolean isFormEnabled()
    {
        return formEnabled;
    }
}
