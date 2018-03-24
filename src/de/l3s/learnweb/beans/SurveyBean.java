package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Survey;
import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.SurveyMetaDataFields;
import de.l3s.learnweb.User;

@ViewScoped
@ManagedBean
public class SurveyBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = Logger.getLogger(SurveyBean.class);
    private int resourceId; //change this when there is a way to generate Survey type resource
    private String surveyTitle;
    private String description;
    private int organizationId;
    private boolean submitted;
    private boolean update;
    private boolean editable;
    private boolean validDates;

    private Survey sv = new Survey();

    private HashMap<String, String> wrappedAnswers = new HashMap<String, String>();
    private HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
    private ArrayList<SurveyMetaDataFields> questions = new ArrayList<SurveyMetaDataFields>();

    public Survey getSv()
    {
        return sv;
    }

    public void setSv(Survey sv)
    {
        this.sv = sv;
    }

    public SurveyBean()
    {
        // to nothing constructor
    }

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {

            try
            {
                //Resource resource = getLearnweb().getResourceManager().getResource(resource_id);
                //groupId = resource.getGroupId();
                //log(Action.glossary_open, groupId, resourceId);
                getSurvey();
            }
            catch(Exception e)
            {
                resourceId = -1;
                log.error("Couldn't log survey action; resource: ", e);
            }

        }

    }

    @PostConstruct
    public void init()
    {
        try
        {
            resourceId = getParameterInt("resource_id");
        }
        catch(NullPointerException e)
        {
            resourceId = 0;
        }

        if(resourceId > 0)
            getSurvey();

    }

    private void getSurvey()
    {
        User user = getUser();
        if(user == null)
            return;

        SurveyManager sm = getLearnweb().getSurveyManager();
        questions = new ArrayList<SurveyMetaDataFields>();

        try
        {
            sv = sm.getFormQuestions(resourceId, user.getId());
            submitted = sv.isSubmitted();
            questions = sv.getFormQuestions();
            editable = sm.checkIfEditable(resourceId);
            surveyTitle = sv.getSurveyTitle();
            description = sv.getDescription();
            organizationId = sv.getOrganizationId();
            Long currentDate = new Date().getTime();
            if((sv.isSubmitted() && !editable) || (sv.isSubmitted() && editable && (sv.getEnd() == null) || currentDate > sv.getEnd().getTime()))
                addGrowl(FacesMessage.SEVERITY_ERROR, "survey_submit_error");
            else
            {
                addGrowl(FacesMessage.SEVERITY_WARN, "survey_submit_edit", sv.getEnd());
            }

        }
        catch(Exception e)
        {
            log.error("Error in fetching form questions for survey: " + resourceId, e);
        }
    }

    //to get pre-filled/submitted survey
    public void getSurvey(int userId)
    {

        SurveyManager sm = getLearnweb().getSurveyManager();
        questions = new ArrayList<SurveyMetaDataFields>();
        try
        {
            sv = new Survey();
            sv = sm.getAssessmentFormDetails(resourceId, userId);
            resourceId = sv.getResourceId();
            update = true;
            questions.clear();
            questions = sv.getFormQuestions();
            surveyTitle = sv.getSurveyTitle();
            wrappedAnswers.clear();
            wrappedAnswers.putAll(sv.getWrappedAnswers());
            wrappedMultipleAnswers.clear();
            wrappedMultipleAnswers = sv.getWrappedMultipleAnswers();

        }
        catch(Exception e)
        {
            log.error("Error in fetching assessment form details for survey :" + resourceId, e);
        }

    }

    public void submit()
    {
        User u = getUser();

        if(!sv.isSubmitted() || update)
        {
            try
            {
                getLearnweb().getSurveyManager().uploadAnswers(u.getId(), wrappedAnswers, wrappedMultipleAnswers, resourceId, update);
            }
            catch(Exception e)
            {
                log.error("Error in uploading answers for User: " + u.getId() + " for survey: " + sv.getSurveyId());
            }
        }
        if(update)
            update = false;
        if(!sv.isSubmitted())
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "submit_survey");
            sv.isSubmitted();
        }
        else if(sv.isSubmitted() && !update && !editable)
            addGrowl(FacesMessage.SEVERITY_ERROR, "survey_submit_error");

    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resource_id)
    {
        this.resourceId = resource_id;
    }

    public HashMap<String, String> getWrappedAnswers()
    {
        return wrappedAnswers;
    }

    public void setWrappedAnswers(HashMap<String, String> wrappedAnswers)
    {
        this.wrappedAnswers = wrappedAnswers;
    }

    public HashMap<String, String[]> getWrappedMultipleAnswers()
    {
        return wrappedMultipleAnswers;
    }

    public void setWrappedMultipleAnswers(HashMap<String, String[]> wrappedMultipleAnswers)
    {
        this.wrappedMultipleAnswers = wrappedMultipleAnswers;
    }

    public ArrayList<SurveyMetaDataFields> getQuestions()
    {
        return questions;
    }

    public void setQuestions(ArrayList<SurveyMetaDataFields> questions)
    {
        this.questions = questions;
    }

    public String getSurveyTitle()
    {
        return surveyTitle;
    }

    public void setSurveyTitle(String surveyTitle)
    {
        this.surveyTitle = surveyTitle;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getOrganizationId()
    {
        return organizationId;
    }

    public void setOrganizationId(int organizationId)
    {
        this.organizationId = organizationId;
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

    public boolean isUpdate()
    {
        return update;
    }

    public void setUpdate(boolean update)
    {
        this.update = update;
    }

    public boolean isEditable()
    {
        return editable;
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    public boolean isValidDates()
    {
        Long currentDate = new Date().getTime();
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

    }

    public void setValidDates(boolean validDates)
    {
        this.validDates = validDates;
    }

}
