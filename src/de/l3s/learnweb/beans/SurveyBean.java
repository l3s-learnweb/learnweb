package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Survey;
import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.User;

@ViewScoped
@ManagedBean
public class SurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = Logger.getLogger(SurveyBean.class);
    private int resourceId;

    private boolean editable;
    private boolean submitted;

    private Survey survey;
    private Resource surveyResource;

    public SurveyBean()
    {
        // do nothing constructor
    }

    public void onLoad()
    {
        User user = getUser();
        if(user == null)
            return;

        if(resourceId > 0)
        {
            try
            {
                surveyResource = getLearnweb().getResourceManager().getResource(resourceId);

                if(!surveyResource.canViewResource(user))
                {
                    addMessage(FacesMessage.SEVERITY_ERROR, "group_resources_access_denied");
                    return;
                }

                //load survey
                SurveyManager sm = getLearnweb().getSurveyManager();

                // survey = sm.getFormQuestions(resourceId, user.getId());
                survey = sm.getAssessmentFormDetails(resourceId, user.getId()); // TODO why does it use another method than in onLoad()

                editable = survey.isEditable() && isValidSubmissionDate(survey);

                submitted = survey.isSubmitted();
                if(survey.isEditable())
                {
                    submitted = getLearnweb().getSurveyManager().getSurveyResourceSubmitStauts(resourceId, getUser().getId());
                }

                if(submitted)
                    addMessage(FacesMessage.SEVERITY_ERROR, "survey_submit_error");
                else if(!isValidSubmissionDate(survey))
                    addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_error_between", survey.getStart(), survey.getEnd());
                else if(survey.isSubmitted())
                {
                    if(survey.getEnd() != null)
                        addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit_until", survey.getEnd());
                    else
                        addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit");
                }
            }
            catch(Exception e)
            {
                resourceId = -1;
                addFatalMessage("Couldn't load survey; resource: ", e);
            }
        }
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public void onSubmit()
    {
        onSave();

        try
        {
            getLearnweb().getSurveyManager().setSurveyResourceSubmitStauts(resourceId, getUser().getId(), true);
            log(Action.survey_submit, surveyResource.getGroupId(), resourceId);
            submitted = true;
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void onSave()
    {
        User u = getUser();

        if(!submitted)
        {
            try
            {
                getLearnweb().getSurveyManager().uploadAnswers(u.getId(), survey.getWrappedAnswers(), survey.getWrappedMultipleAnswers(), resourceId);

                addMessage(FacesMessage.SEVERITY_INFO, "submit_survey");
                survey.setSubmitted(true);

                log(Action.survey_save, surveyResource.getGroupId(), resourceId);
            }
            catch(Exception e)
            {
                addFatalMessage("Error in uploading answers for User: " + u.getId() + " for survey: " + survey.getSurveyId(), e);
            }
        }
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resource_id)
    {
        this.resourceId = resource_id;
    }

    public boolean isEditable()
    {
        return editable;
    }

    private static boolean isValidSubmissionDate(Survey survey)
    {
        Long currentDate = new Date().getTime();

        if(survey.getStart() != null && survey.getStart().getTime() > currentDate)
            return false;
        if(survey.getEnd() != null && survey.getEnd().getTime() < currentDate)
            return false;
        return true;

        /*

        if(sv.getStart() == null && sv.getEnd() == null)
        {
            // Both Dates not set
            return true;
        }
        else if(sv.getStart() != null && sv.getEnd() != null) //Both dates are set
            return (sv.getStart().getTime() <= currentDate && sv.getEnd().getTime() >= currentDate ? true : false);
        else if(sv.getStart() != null && sv.getStart().getTime() <= currentDate)
            return true;
        else if(sv.getEnd() != null && sv.getEnd().getTime() >= currentDate)
            return true;
        else
            return false;
            */

    }
}
