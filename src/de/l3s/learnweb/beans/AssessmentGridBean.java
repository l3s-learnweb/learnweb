package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.SurveyMetaDataFields;
import de.l3s.learnweb.SurveyResource;
import de.l3s.learnweb.SurveyUserAnswers;
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
    private HashMap<Integer, String> wrappedAnswers = new HashMap<>();

    private HashMap<Integer, String[]> wrappedMultipleAnswers = new HashMap<>();
    private ArrayList<SurveyMetaDataFields> questions = new ArrayList<SurveyMetaDataFields>();
    private int userId = 0;
    private SurveyResource sv; // TODO dont initialize

    private List<User> users; // TODO dont initialize
    private SurveyUserAnswers surveyAnswer;

    public AssessmentGridBean()
    {
    }

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(getUser() == null || (!getUser().isModerator() && getUser().getId() != getUserId()))
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "group_resources_access_denied");
            return;
        }

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
            SurveyResource surveyResource = (SurveyResource) getLearnweb().getResourceManager().getResource(resourceId);
            surveyAnswer = surveyResource.getAnswersOfUser(userId);
            sv = surveyResource;
            submitted = surveyAnswer.isSaved();
            questions = surveyResource.getQuestions();
            surveyTitle = surveyResource.getTitle();
            wrappedAnswers = surveyAnswer.getAnswers();
            wrappedMultipleAnswers = surveyAnswer.getMultipleAnswers();
        }
        catch(Exception e)
        {
            log.error("Error in fetching assessment form details for survey :" + resourceId, e);
        }

    }

    public SurveyUserAnswers getSurveyAnswer()
    {
        return surveyAnswer;
    }

    public SurveyResource getSv() // TODO rename to getSurvey
    {
        return sv;
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

    public HashMap<Integer, String> getWrappedAnswers()
    {
        return wrappedAnswers;
    }

    public void setWrappedAnswers(HashMap<Integer, String> wrappedAnswers)
    {
        this.wrappedAnswers = wrappedAnswers;
    }

    public HashMap<Integer, String[]> getWrappedMultipleAnswers()
    {
        return wrappedMultipleAnswers;
    }

    public void setWrappedMultipleAnswers(HashMap<Integer, String[]> wrappedMultipleAnswers)
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
