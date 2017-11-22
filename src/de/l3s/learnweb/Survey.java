package de.l3s.learnweb;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;

public class Survey
{
    public boolean isSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

    public Date getStart()
    {
        return start;
    }

    public void setStart(Date start)
    {
        this.start = start;
    }

    public Date getEnd()
    {
        return end;
    }

    public void setEnd(Date end)
    {
        this.end = end;
    }

    public int getSurvey_id()
    {
        return survey_id;
    }

    public void setSurvey_id(int survey_id)
    {
        this.survey_id = survey_id;
    }

    public int getResource_id()
    {
        return resource_id;
    }

    public void setResource_id(int resource_id)
    {
        this.resource_id = resource_id;
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

    public ArrayList<SurveyMetaDataFields> getFormQuestions()
    {
        return formQuestions;
    }

    public void setFormQuestions(ArrayList<SurveyMetaDataFields> formQuestions)
    {
        this.formQuestions = formQuestions;
    }

    public int getOrganizationId()
    {
        return organizationId;
    }

    public void setOrganizationId(int organizationId)
    {
        this.organizationId = organizationId;
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


    boolean submitted = false;

    Date start;
    Date end;
    int survey_id;
    int resource_id;
    String surveyTitle;
    String description;
    int organizationId;
    ArrayList<SurveyMetaDataFields> formQuestions = new ArrayList<SurveyMetaDataFields>();
    HashMap<String, String> wrappedAnswers = new HashMap<String, String>();
    HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
}
