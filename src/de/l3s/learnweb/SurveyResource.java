package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SurveyResource extends Resource implements Serializable
{
    private static final long serialVersionUID = 3431955030925189235L;

    private int surveyId;
    private Date start;
    private Date end;
    private boolean saveable; // if true users can save before their answers before finally submitting them

    private Survey survey;

    /**
     * Do nothing constructor
     */
    public SurveyResource()
    {
    }

    /**
     * Copy constructor
     *
     * @param old
     */
    public SurveyResource(SurveyResource old)
    {
        super(old);
        setSurveyId(old.getSurveyId());
        setStart(old.getStart());
        setEnd(old.getEnd());
        setSaveable(old.isSaveable());
    }

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

    public List<User> getUsersWhoSaved() throws SQLException
    {
        return Learnweb.getInstance().getSurveyManager().getUsersWhoSavedSurveyResource(getId());
    }

    @Override
    public Resource save() throws SQLException
    {
        // save normal resource fields
        super.save();

        // save SurveyResourceFields
        Learnweb.getInstance().getSurveyManager().saveSurveyResource(this);

        return this;
    }

    @Override
    public SurveyResource clone()
    {
        return new SurveyResource(this);
    }

    @Override
    public String toString()
    {
        return "Survey" + super.toString();
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

    /**
     *
     * @return True if the form can be saved before submit
     */
    public boolean isSaveable()
    {
        return saveable;
    }

    public void setSaveable(boolean editable)
    {
        this.saveable = editable;
    }

}
