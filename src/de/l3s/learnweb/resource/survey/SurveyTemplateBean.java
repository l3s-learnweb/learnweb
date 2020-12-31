package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

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

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        if (resourceId != 0) {
            SurveyResource surveyResource = dao().getSurveyDao().findResourceById(resourceId).orElseThrow(BeanAssert.NOT_FOUND);
            surveyId = surveyResource.getSurveyId();
        }

        survey = dao().getSurveyDao().findById(surveyId).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.notDeleted(survey);
        BeanAssert.hasPermission(getUser().isAdmin() || getUser().getId() == survey.getUserId());
    }

    public void onSave() {
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

    public void onAddEmptyQuestion() {
        List<SurveyQuestion> questions = getSurvey().getQuestions();
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.INPUT_TEXT, surveyId);
        question.setOrder(questions.size());
        setCurrentQuestion(question);
    }

    public void onAddQuestion() {
        getSurvey().getQuestions().add(currentQuestion);
    }

    public void onMoveQuestionUp(SurveyQuestion question) {
        onMoveQuestion(question, -1);
    }

    public void onMoveQuestionDown(SurveyQuestion question) {
        onMoveQuestion(question, 1);
    }

    /**
     * @param direction set 1 to move upward or -1 to move down
     */
    private void onMoveQuestion(SurveyQuestion question, int direction) {
        int oldOrder = question.getOrder();
        question.setOrder(oldOrder + direction); // move selected question

        SurveyQuestion neighbor = getSurvey().getQuestions().get(question.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        getSurvey().getQuestions().sort(Comparator.comparingInt(SurveyQuestion::getOrder));
    }

    public void onDeleteQuestion(SurveyQuestion question) {
        question.setDeleted(true);
    }

    public List<SurveyQuestion> getQuestions() {
        return survey.getQuestions().stream().filter(question -> !question.isDeleted()).collect(Collectors.toList());
    }

    public Survey getSelectedSurvey() {
        return selectedSurvey;
    }

    public void setSelectedSurvey(Survey selectedSurvey) {
        this.selectedSurvey = selectedSurvey;
    }

    public List<Survey> getSurveys() {
        if (surveys == null) {
            surveys = new ArrayList<>();
            List<Survey> allSurveysPerOrganisation = dao().getSurveyDao().findByOrganisationIdOrUserId(getUser().getOrganisationId(), getUser().getId());
            for (Survey survey : allSurveysPerOrganisation) {
                if (survey.isPublicTemplate() || getUser().getId() == survey.getUserId() && survey.getId() != this.survey.getId()) {
                    surveys.add(survey);
                }
            }
        }
        return surveys;
    }

    public void onCopyQuestions() {
        for (SurveyQuestion question : selectedSurvey.getQuestions()) {
            survey.getQuestions().add(new SurveyQuestion(question));
        }
    }
}
