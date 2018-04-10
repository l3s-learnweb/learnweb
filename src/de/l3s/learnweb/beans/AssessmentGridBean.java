package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Survey;
import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.SurveyMetaDataFields;
import de.l3s.learnweb.User;

// TODO the whole class can be removed. use surveybean instead
@Deprecated
@ViewScoped
@ManagedBean
public class AssessmentGridBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 4308612517681227920L;
    private static final Logger log = Logger.getLogger(AssessmentGridBean.class);
    private int resourceId = 0; //change this when there is a way to generate Survey type resource
    private String title;
    private String surveyTitle;
    private String description;
    private int organizationId;
    private boolean submitted;
    private HashMap<String, String> wrappedAnswers = new HashMap<String, String>();

    private HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
    private ArrayList<SurveyMetaDataFields> questions = new ArrayList<SurveyMetaDataFields>();
    private int userId = 0;
    private Survey sv = new Survey(); // TODO dont initialize

    private List<User> users = new ArrayList<User>(); // TODO dont initialize

    public AssessmentGridBean()
    {
    }

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {

            try
            {

                if(userId > 0)
                    getSurvey();
                else
                    getAssessmentUsers();

                // getSurvey();
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
        try
        {
            userId = getParameterInt("user_id");
        }
        catch(NullPointerException e)
        {
            userId = 0;
        }

        if(userId > 0)
        {
            getSurvey();
        }
        else if(resourceId > 0)
        {
            //getSurvey();
            try
            {
                title = getLearnweb().getResourceManager().getResource(resourceId).getTitle();
            }
            catch(SQLException e)
            {
                log.warn("Couldn't fetch assessment grid title for resource id: " + resourceId);
            }
            getAssessmentUsers();
        }

    }

    private void getAssessmentUsers()
    {

        users = new ArrayList<User>();

        SurveyManager assessManager = getLearnweb().getSurveyManager();
        try
        {
            users = assessManager.getSurveyUsers(resourceId);
        }
        catch(Exception e)
        {
            log.error("Error in fetching users who answered survey: " + resourceId);

        }

    }

    public void getSurvey()
    {

        SurveyManager sm = getLearnweb().getSurveyManager();
        questions = new ArrayList<SurveyMetaDataFields>();
        try
        {
            sv = sm.getAssessmentFormDetails(resourceId, userId);
            submitted = sv.isSubmitted();
            questions = sv.getFormQuestions();
            surveyTitle = sv.getSurveyTitle();
            wrappedAnswers = sv.getWrappedAnswers();
            wrappedMultipleAnswers = sv.getWrappedMultipleAnswers();
        }
        catch(Exception e)
        {
            log.error("Error in fetching assessment form details for survey :" + resourceId, e);
        }

    }

    public Survey getSv() // TODO rename to getSurvey
    {
        return sv;
    }

    public void setSv(Survey sv)
    {
        this.sv = sv;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resource_id)
    {
        this.resourceId = resource_id;
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

    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers(List<User> users)
    {

        this.users = users;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
