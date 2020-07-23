package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;

@Named
@ViewScoped
public class SurveyTemplateBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 669288862248912801L;

    private int surveyId;
    private int resourceId;
    private Survey survey;
    private SurveyQuestion currentQuestion;
    private List<Survey> surveys;
    private Survey selectedSurvey;

    public SurveyTemplateBean() {
    }

    public void onLoad() throws SQLException {
        BeanAssert.authorized(isLoggedIn());

        if (resourceId != 0) {
            SurveyResource surveyResource = getLearnweb().getSurveyManager().getSurveyResource(resourceId);
            surveyId = surveyResource.getSurveyId();
        }

        if (surveyId != 0) { // edit an existing survey
            survey = getLearnweb().getSurveyManager().getSurvey(surveyId);
        }

        BeanAssert.isFound(survey);
        BeanAssert.notDeleted(survey);
        BeanAssert.hasPermission(getUser().isAdmin() || getUser().getId() == survey.getUserId());
    }

    public void onSave() throws SQLException {
        survey.save(false);
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public SurveyQuestion getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(SurveyQuestion currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public void onAddEmptyAnswer() {
        this.currentQuestion.getAnswers().add(new SurveyQuestionOption());
    }

    public void onDeleteAnswer(SurveyQuestionOption answer) {
        answer.setDeleted(true);
    }

    public Survey getSurvey() {
        return survey;
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(final int surveyId) {
        this.surveyId = surveyId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(final int resourceId) {
        this.resourceId = resourceId;
    }

    public void onAddEmptyQuestion() throws SQLException {
        List<SurveyQuestion> questions = getSurvey().getQuestions();
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.INPUT_TEXT, surveyId);
        question.setOrder(questions.size());
        setCurrentQuestion(question);
    }

    public void onAddQuestion() throws SQLException {
        getSurvey().getQuestions().add(currentQuestion);
    }

    public void onMoveQuestionUp(SurveyQuestion question) throws SQLException {
        onMoveQuestion(question, -1);
    }

    public void onMoveQuestionDown(SurveyQuestion question) throws SQLException {
        onMoveQuestion(question, 1);
    }

    /**
     * @param direction set 1 to move upward or -1 to move down
     */
    private void onMoveQuestion(SurveyQuestion question, int direction) throws SQLException {
        int oldOrder = question.getOrder();
        question.setOrder(oldOrder + direction); // move selected question

        SurveyQuestion neighbor = getSurvey().getQuestions().get(question.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        getSurvey().getQuestions().sort(Comparator.comparingInt(SurveyQuestion::getOrder));
    }

    public void onDeleteQuestion(SurveyQuestion question) {
        question.setDeleted(true);
    }

    public List<SurveyQuestion> getQuestions() throws SQLException {
        return survey.getQuestions().stream().filter(question -> !question.isDeleted()).collect(Collectors.toList());
    }

    public Survey getSelectedSurvey() {
        return selectedSurvey;
    }

    public void setSelectedSurvey(Survey selectedSurvey) {
        this.selectedSurvey = selectedSurvey;
    }

    public List<Survey> getSurveys() throws SQLException {
        if (surveys == null) {
            surveys = new ArrayList<>();
            List<Survey> allSurveysPerOrganisation = getLearnweb().getSurveyManager().getPublicSurveysByOrganisationOrUser(getUser());
            for (Survey survey : allSurveysPerOrganisation) {
                if (survey.isPublicTemplate() || getUser().getId() == survey.getUserId() && survey.getId() != this.survey.getId()) {
                    surveys.add(survey);
                }
            }
        }
        return surveys;
    }

    public void onCopyQuestions() throws SQLException {
        for (SurveyQuestion question : selectedSurvey.getQuestions()) {
            survey.getQuestions().add(question.clone());
        }
    }
}
