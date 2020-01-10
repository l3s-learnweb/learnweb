package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class SurveyTemplateBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 669288862248912801L;

    private int surveyId;
    private Survey survey;
    private SurveyQuestion currentQuestion;

    public SurveyTemplateBean()
    {
    }

    public void onLoad() throws SQLException
    {
        if(!isLoggedIn())
            return;

        if(surveyId != 0) // edit an existing survey
            survey = getLearnweb().getSurveyManager().getSurvey(surveyId);

        if(null == survey)
            addInvalidParameterMessage("survey_id");
    }

    public void onSave() throws SQLException
    {
        getLearnweb().getSurveyManager().save(survey);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public SurveyQuestion getCurrentQuestion()
    {
        return currentQuestion;
    }

    public void setCurrentQuestion(SurveyQuestion currentQuestion)
    {
        this.currentQuestion = currentQuestion;
    }

    public void onAddEmptyAnswer()
    {
        this.currentQuestion.getAnswers().add(new SurveyQuestionOption());
    }

    public void onDeleteAnswer(SurveyQuestionOption answer)
    {
        answer.setDeleted(true);
    }

    public Survey getSurvey()
    {
        return survey;
    }

    public int getSurveyId()
    {
        return surveyId;
    }

    public void setSurveyId(final int surveyId)
    {
        this.surveyId = surveyId;
    }

    public void onAddEmptyQuestion()
    {
        List<SurveyQuestion> questions = getSurvey().getQuestions();
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.INPUT_TEXT, surveyId);
        question.setOrder(questions.size());
        survey.addQuestion(question);
        setCurrentQuestion(question);
    }

    public void onMoveQuestionUp(SurveyQuestion question)
    {
        onMoveQuestion(question, -1);
    }

    public void onMoveQuestionDown(SurveyQuestion question)
    {
        onMoveQuestion(question, 1);
    }

    /**
     *
     * @param question
     * @param direction set 1 to move upward or -1 to move down
     * @throws SQLException
     */
    private void onMoveQuestion(SurveyQuestion question, int direction)
    {
        int oldOrder = question.getOrder();
        question.setOrder(oldOrder + direction); // move selected question

        SurveyQuestion neighbor = getSurvey().getQuestions().get(question.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        getSurvey().getQuestions().sort(Comparator.comparingInt(SurveyQuestion::getOrder));
    }

    public void onDeleteQuestion(SurveyQuestion question)
    {
        question.setDeleted(true);
    }

    public boolean isSurveyAssociatedWithResource() throws SQLException
    {
        return getLearnweb().getSurveyManager().isSurveyAssociatedWithResource(surveyId);
    }

    public List<SurveyQuestion> getQuestions()
    {
        return survey.getQuestions().stream().filter(question -> !question.isDeleted()).collect(Collectors.toList());
    }

    public boolean isCreator()
    {
        return getUser().getId() == survey.getUserId();
    }
}
