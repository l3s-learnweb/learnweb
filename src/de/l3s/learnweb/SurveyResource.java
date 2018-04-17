package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

public class SurveyResource extends Resource implements Serializable
{
    private static final long serialVersionUID = 3431955030925189235L;

    private int surveyId;
    private Date start;
    private Date end;
    private boolean editable; // if true users can save before their answers before finally submitting them

    private Survey survey;

    @Override
    protected void postConstruct() throws SQLException
    {
        super.postConstruct();

        Learnweb.getInstance().getSurveyManager().loadSurveyResource(this);
    }

    public Survey getSurvey() throws SQLException
    {
        if(null == survey)
            survey = Learnweb.getInstance().getSurveyManager().getSurvey(surveyId);
        return survey;
    }

    public ArrayList<SurveyMetaDataFields> getQuestions() throws SQLException
    {
        return getSurvey().getQuestions();
    }

    public SurveyUserAnswers getAnswersOfUser(int userId) throws SQLException
    {
        return Learnweb.getInstance().getSurveyManager().getAnswersOfUser(getSurvey(), getId(), userId);
    }

    public int getSurveyId()
    {
        return surveyId;
    }

    public void setSurveyId(int surveyId)
    {
        this.surveyId = surveyId;
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

    public boolean isEditable()
    {
        return editable;
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

}
