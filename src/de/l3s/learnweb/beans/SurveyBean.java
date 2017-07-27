package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SurveyManager;
import de.l3s.learnweb.SurveyMetaDataFields;
import de.l3s.learnweb.User;

@ViewScoped
@ManagedBean
public class SurveyBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = -6217166153267996666L;
    private static final Logger log = Logger.getLogger(SurveyBean.class);
    private int resource_id = 123; //change this when there is a way to generate Survey type resource
    private String surveyTitle;
    private String description;
    private HashMap<String, String> wrappedAnswers = new HashMap<String, String>();

    private HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
    private ArrayList<SurveyMetaDataFields> questions;

    @PostConstruct
    public void init()
    {

        getSurvey();

    }

    private void getSurvey()
    {
        SurveyManager sm = getLearnweb().getSurveyManager();
        questions = sm.getFormQuestions(resource_id);
        surveyTitle = sm.getSurveyTitle();
        description = sm.getDescription();

    }

    public void submit()
    {
        User u = getUser();
        for(Entry<String, String[]> e : wrappedMultipleAnswers.entrySet())
        {
            System.out.println(e.getKey());
            System.out.println(e.getValue().length);
        }
        getLearnweb().getSurveyManager().upload(u.getId(), wrappedAnswers, wrappedMultipleAnswers);
        FacesContext context1 = FacesContext.getCurrentInstance();

        context1.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info: ", "Successful Submit"));
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
}
