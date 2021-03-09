package de.l3s.learnweb.resource.survey;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.user.User;
import de.l3s.util.Cache;

public class SurveyResource extends Resource {
    private static final long serialVersionUID = 3431955030925189235L;

    private int surveyId;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean saveable; // if true users can save their answers before finally submitting them

    private Survey survey;

    private transient Cache<SurveyUserAnswers> answerCache;

    /**
     * Do nothing constructor.
     */
    public SurveyResource() {
        super(LEARNWEB_RESOURCE, ResourceType.survey, ResourceService.learnweb);
    }

    /**
     * Copy constructor.
     */
    public SurveyResource(SurveyResource other) {
        super(other);
        setSurveyId(other.getSurveyId());
        setStart(other.getStart());
        setEnd(other.getEnd());
        setSaveable(other.isSaveable());
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        answerCache = null;
        survey = null;
    }

    @Override
    protected void postConstruct() {
        super.postConstruct();

        Learnweb.dao().getSurveyDao().loadSurveyResource(this);
    }

    public Survey getSurvey() {
        if (null == survey) {
            survey = Learnweb.dao().getSurveyDao().findById(surveyId).orElseThrow(BeanAssert.NOT_FOUND);
        }
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
        this.surveyId = survey.getId();
    }

    public Collection<SurveyQuestion> getQuestions() {
        return getSurvey().getQuestions();
    }

    private Cache<SurveyUserAnswers> getAnswerCache() {
        if (null == answerCache) {
            answerCache = new Cache<>(1000);
        }
        return answerCache;
    }

    /**
     * @return true if this user has submitted this survey
     */
    public boolean isSubmitted(int userId) {
        return Learnweb.dao().getSurveyDao().findSubmittedStatus(this.getId(), userId).orElse(false);
    }

    /**
     * Returns all answers of user even when they are incomplete or not final.
     */
    public List<SurveyUserAnswers> getAnswersOfAllUsers() {
        return getAnswers(false);
    }

    /**
     * Returns only answers that were finally submitted.
     */
    public List<SurveyUserAnswers> getSubmittedAnswersOfAllUsers() {
        return getAnswers(true);
    }

    private List<SurveyUserAnswers> getAnswers(boolean returnOnlySubmittedAnswers) {
        List<SurveyUserAnswers> answers = new LinkedList<>();

        List<User> users = returnOnlySubmittedAnswers ? Learnweb.dao().getUserDao().findBySubmittedSurveyResourceId(getId())
            : Learnweb.dao().getUserDao().findBySavedSurveyResourceId(getId());

        for (User user : users) {
            answers.add(getAnswersOfUser(user.getId()));
        }

        return answers;
    }

    public SurveyUserAnswers getAnswersOfUser(int userId) {
        SurveyUserAnswers answers = getAnswerCache().get(userId);
        if (null == answers) {
            answers = Learnweb.dao().getSurveyDao().findAnswersByResourceAndUserId(this, userId);
            getAnswerCache().put(answers);
        }
        return answers;
    }

    public List<User> getUsersWhoSaved() {
        return Learnweb.dao().getUserDao().findBySavedSurveyResourceId(getId());
    }

    public List<User> getUsersWhoSubmitted() {
        return Learnweb.dao().getUserDao().findBySubmittedSurveyResourceId(getId());
    }

    @Override
    public Resource save() {
        // save normal resource fields
        super.save();
        // save SurveyResource fields
        if (surveyId == 0) {
            survey.save(true);
            surveyId = survey.getId();
        }

        Learnweb.dao().getSurveyDao().saveSurveyResource(this);
        return this;
    }

    @Override
    public String toString() {
        return "Survey" + super.toString();
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    /**
     * @return True if the form can be saved before submit
     */
    public boolean isSaveable() {
        return saveable;
    }

    public void setSaveable(boolean editable) {
        this.saveable = editable;
    }

}
