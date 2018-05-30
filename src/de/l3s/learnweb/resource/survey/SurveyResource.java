package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User;
import de.l3s.util.Cache;

public class SurveyResource extends Resource implements Serializable
{
    private static final long serialVersionUID = 3431955030925189235L;

    private int surveyId;
    private Date start = null;
    private Date end = null;
    private boolean saveable; // if true users can save before their answers before finally submitting them

    private Survey survey;

    private transient Cache<SurveyUserAnswers> answerCache;

    /**
     * Do nothing constructor
     */
    public SurveyResource()
    {
    }

    @Override
    public void clearCaches()
    {
        super.clearCaches();
        answerCache = null;
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

    public ArrayList<SurveyQuestion> getQuestions() throws SQLException
    {
        return getSurvey().getQuestions();
    }

    private Cache<SurveyUserAnswers> getAnswerCache()
    {
        if(null == answerCache)
            answerCache = new Cache<SurveyUserAnswers>(50);
        return answerCache;
    }

    /**
     *
     * @param userId
     * @return true if this user has submitted this survey
     * @throws SQLException
     */
    public boolean isSubmitted(int userId) throws SQLException
    {
        return Learnweb.getInstance().getSurveyManager().getSurveyResourceSubmitStatus(this.getId(), userId);
    }

    public SurveyUserAnswers getAnswersOfUser(int userId) throws SQLException
    {
        SurveyUserAnswers answers = getAnswerCache().get(userId);
        if(null == answers)
        {
            answers = Learnweb.getInstance().getSurveyManager().getAnswersOfUser(getSurvey(), getId(), userId);
            getAnswerCache().put(answers);
        }
        return answers;
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
