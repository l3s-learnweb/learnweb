package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
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
    private int surveyUserId; // the user whose answers are viewed, by default the currently loggedin user

    private boolean editable;

    private SurveyResource resource;
    private SurveyUserAnswers userAnswers;

    public void onLoad()
    {
        User user = getUser();
        if(user == null)
            return;

        if(surveyResourceId <= 0)
        {
            addInvalidParameterMessage("resource_id");
            return;
        }

        try
        {
            resource = (SurveyResource) getLearnweb().getResourceManager().getResource(surveyResourceId);

            if(!resource.canViewResource(user))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "group_resources_access_denied");
                return;
            }

            // whose answers shall be view
            if(surveyUserId <= 0) // by efault view the answers of the current user
                surveyUserId = getUser().getId();
            else if(!resource.canModerateResource(getUser()))
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to view the answers of the given user");
                log.warn("Illegal access: " + BeanHelper.getRequestSummary());
                resource = null;
                return;
            }

            editable = resource.isEditable() && isValidSubmissionDate(resource);
            userAnswers = resource.getAnswersOfUser(surveyUserId);

            if(isSubmitted())
                addMessage(FacesMessage.SEVERITY_ERROR, "survey_submit_error");
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
        catch(Exception e)
        {
            surveyResourceId = -1;
            addFatalMessage("Couldn't load survey; resource: ", e);
        }

    }

    public boolean isSubmitted()
    {
        return userAnswers.isSubmitted();
    }

    private boolean onSaveOrSubmit(boolean submit)
    {
        if(isSubmitted())
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "This Survey has already been submitted");
            log.error("Survey already submitted. Should not happen. For User: " + surveyUserId + " for survey: " + surveyResourceId);
            return false;
        }

        try
        {
            getLearnweb().getSurveyManager().saveAnswers(userAnswers, submit);

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
            addMessage(FacesMessage.SEVERITY_INFO, "submit_survey");
            log(Action.survey_submit, resource.getGroupId(), surveyResourceId);
        }
    }

    public void onSave()
    {
        if(onSaveOrSubmit(false))
        {
            addMessage(FacesMessage.SEVERITY_INFO, "submit_save");
            log(Action.survey_save, resource.getGroupId(), surveyResourceId);
        }
    }

    public boolean isEditable()
    {
        return editable;
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
}
