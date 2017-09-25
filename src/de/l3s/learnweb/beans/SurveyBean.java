package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.ArrayList;
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
    private int resource_id; //change this when there is a way to generate Survey type resource
    private String surveyTitle;
    private String description;
    private int organizationId;
    private boolean submitted;
    private Survey sv = new Survey();

    public Survey getSv()
    {
        return sv;
    }

    public void setSv(Survey sv)
    {
        this.sv = sv;
    }

    private HashMap<String, String> wrappedAnswers = new HashMap<String, String>();

    private HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
    private ArrayList<SurveyMetaDataFields> questions = new ArrayList<SurveyMetaDataFields>();

    public SurveyBean()
    {
        System.out.println("Constructor is initialized");
    }

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resource_id > 0)
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
                log.error("Couldn't log survey action; resource: ", e);
            }

        }

    }

    @PostConstruct
    public void init()
    {
        resource_id = getParameterInt("resource_id");
        if(resource_id > 0)
            getSurvey();

    }

    private void getSurvey()
    {
        SurveyManager sm = getLearnweb().getSurveyManager();
        questions = new ArrayList<SurveyMetaDataFields>();
        User user = getUser();
        if(user == null)
            return;

        sv = sm.getFormQuestions(resource_id, user.getId());
        submitted = sv.isSubmitted();
        questions = sv.getFormQuestions();

        surveyTitle = sv.getSurveyTitle();
        description = sv.getDescription();
        organizationId = sv.getOrganizationId();
        if(sv.isSubmitted())
            addGrowl(FacesMessage.SEVERITY_ERROR, "You have submitted the form previously. You can only submit once.");
    }

    public void submit()
    {
        User u = getUser();

        getLearnweb().getSurveyManager().upload(u.getId(), wrappedAnswers, wrappedMultipleAnswers, resource_id);

        if(!sv.isSubmitted())
        {
            addGrowl(FacesMessage.SEVERITY_INFO, "Successful Submit");
            sv.isSubmitted();
        }
        else
            addGrowl(FacesMessage.SEVERITY_ERROR, "You have submitted the form previously. You can only submit once.");

    }

    public int getResource_id()
    {
        return resource_id;
    }

    public void setResource_id(int resource_id)
    {
        this.resource_id = resource_id;
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
}
