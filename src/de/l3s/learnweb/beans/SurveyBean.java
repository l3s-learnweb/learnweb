package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Survey;
import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.User;

@ViewScoped
@ManagedBean
public class SurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6217166153267996666L;
    //private static final Logger log = Logger.getLogger(SurveyBean.class);
    private int resourceId;

    private boolean update;
    private boolean editable;

    private Survey survey;

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
                Resource resource = getLearnweb().getResourceManager().getResource(resourceId);
                if(!resource.canViewResource(user))
                {
                    addMessage(FacesMessage.SEVERITY_ERROR, "group_resources_access_denied");
                    return;
                }

                //load survey
                SurveyManager sm = getLearnweb().getSurveyManager();

                survey = sm.getFormQuestions(resourceId, user.getId());
                editable = survey.isEditable() && isValidSubmissionDate(survey);

                if(!isValidSubmissionDate(survey))
                    addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_error_between", survey.getStart(), survey.getEnd());
                else if(survey.isSubmitted())
                {
                    if(editable && survey.getEnd() != null)
                        addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit_until", survey.getEnd());
                    else if(editable)
                        addMessage(FacesMessage.SEVERITY_WARN, "survey_submit_edit");
                    else if(!editable)
                        addMessage(FacesMessage.SEVERITY_ERROR, "survey_submit_error");
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

    //to get pre-filled/submitted survey
    public void getSurvey(int userId)
    {

        SurveyManager sm = getLearnweb().getSurveyManager();
        try
        {
            survey = new Survey();
            survey = sm.getAssessmentFormDetails(resourceId, userId); // TODO why does it use another method than in onLoad()
            resourceId = survey.getResourceId();
            update = true;

        }
        catch(Exception e)
        {
            addFatalMessage("Error in fetching assessment form details for survey :" + resourceId, e);
        }

    }

    public void submit()
    {
        User u = getUser();

        if(!survey.isSubmitted() || update)
        {
            try
            {
                getLearnweb().getSurveyManager().uploadAnswers(u.getId(), survey.getWrappedAnswers(), survey.getWrappedMultipleAnswers(), resourceId, update);

                addMessage(FacesMessage.SEVERITY_INFO, "submit_survey");
                survey.setSubmitted(true);
                update = false;
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

    public boolean isUpdate()
    {
        return update;
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
