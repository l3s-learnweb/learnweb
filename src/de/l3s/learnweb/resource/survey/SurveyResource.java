package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
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
    private boolean saveable; // if true users can save their answers before finally submitting them

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
        survey = null;
    }

    /**
     * Copy constructor
     *
     * @param other
     */
    public SurveyResource(SurveyResource other)
    {
        super(other);
        setSurveyId(other.getSurveyId());
        setStart(other.getStart());
        setEnd(other.getEnd());
        setSaveable(other.isSaveable());
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

    public Collection<SurveyQuestion> getQuestions() throws SQLException
    {
        return getSurvey().getQuestions();
    }

    private Cache<SurveyUserAnswers> getAnswerCache()
    {
        if(null == answerCache)
            answerCache = new Cache<>(1000);
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

    /**
     * Returns all answers of user even when they are incomplete or not final
     *
     * @return
     * @throws SQLException
     */
    public List<SurveyUserAnswers> getAnswersOfAllUsers() throws SQLException
    {
        return getAnswers(false);
    }

    /**
     * Returns only answers that were finally submitted
     *
     * @return
     * @throws SQLException
     */
    public List<SurveyUserAnswers> getSubmittedAnswersOfAllUsers() throws SQLException
    {
        return getAnswers(true);
    }

    private List<SurveyUserAnswers> getAnswers(boolean returnOnlySubmittedAnswers) throws SQLException
    {
        List<SurveyUserAnswers> answers = new LinkedList<>();

        SurveyManager surveyManager = Learnweb.getInstance().getSurveyManager();

        List<User> users = returnOnlySubmittedAnswers ? surveyManager.getUsersWhoSubmittedSurveyResource(getId()) : surveyManager.getUsersWhoSavedSurveyResource(getId());
        for(User user : users)
        {
            answers.add(getAnswersOfUser(user.getId()));
        }

        return answers;
    }

    public SurveyUserAnswers getAnswersOfUser(int userId) throws SQLException
    {
        SurveyUserAnswers answers = getAnswerCache().get(userId);
        if(null == answers)
        {
            answers = Learnweb.getInstance().getSurveyManager().getAnswersOfUser(this, userId);
            getAnswerCache().put(answers);
        }
        return answers;
    }

    public List<User> getUsersWhoSaved() throws SQLException
    {
        return Learnweb.getInstance().getSurveyManager().getUsersWhoSavedSurveyResource(getId());
    }

    public List<User> getUsersWhoSubmitted() throws SQLException
    {
        return Learnweb.getInstance().getSurveyManager().getUsersWhoSubmittedSurveyResource(getId());
    }

    @Override
    public Resource save() throws SQLException
    {
        // save normal resource fields
        super.save();

        // save SurveyResource fields
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
