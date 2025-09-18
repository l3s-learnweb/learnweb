package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@ViewScoped
public class SurveyPageEditBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -4154316670913339095L;

    private ArrayList<SelectItem> questionTypes;

    @Inject
    private SurveyDao surveyDao;

    public ArrayList<SelectItem> getQuestionTypes() {
        if (questionTypes == null) {
            questionTypes = new ArrayList<>();
            ResourceBundle msg = getBundle();

            SelectItemGroup testGroup = new SelectItemGroup("Text types");
            testGroup.setSelectItems(Stream.of(
                SurveyQuestion.QuestionType.INPUT_TEXT,
                SurveyQuestion.QuestionType.INPUT_TEXTAREA
            ).map(q -> new SelectItem(q.name(), msg.getString("question_type." + q.name()), msg.getString("question_type.desc_" + q.name()))).toArray(SelectItem[]::new));
            questionTypes.add(testGroup);

            SelectItemGroup choiceGroup = new SelectItemGroup("Choice types");
            choiceGroup.setSelectItems(Stream.of(
                SurveyQuestion.QuestionType.ONE_BUTTON,
                SurveyQuestion.QuestionType.ONE_RADIO,
                SurveyQuestion.QuestionType.ONE_MENU,
                SurveyQuestion.QuestionType.ONE_MENU_EDITABLE,
                SurveyQuestion.QuestionType.MANY_CHECKBOX,
                SurveyQuestion.QuestionType.MULTIPLE_MENU
            ).map(q -> new SelectItem(q.name(), msg.getString("question_type." + q.name()), msg.getString("question_type.desc_" + q.name()))).toArray(SelectItem[]::new));
            questionTypes.add(choiceGroup);
        }
        return questionTypes;
    }

    public void onPageChange(SurveyPage page) {
        // log.info("New value: {}", question.getLabel());
        surveyDao.savePage(page);
    }

    public void onQuestionChange(SurveyQuestion question) {
        // log.info("New value: {}", question.getLabel());
        surveyDao.saveQuestion(question);
    }

    public void onQuestionOptionAdd(SurveyQuestion question) {
        question.getOptions().add(new SurveyQuestionOption());
    }

    public void onQuestionOptionChange(SurveyQuestion question, SurveyQuestionOption answer) {
        surveyDao.saveQuestionOption(question.getId(), answer);
    }

    public void onQuestionOptionDelete(SurveyQuestion question, SurveyQuestionOption answer) {
        answer.setDeleted(true);
        surveyDao.saveQuestionOption(question.getId(), answer);
    }

    public void onAddQuestion(SurveyPage page) {
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.INPUT_TEXT);
        question.setOrder(page.getQuestions().size());
        question.setPageId(page.getId());
        page.getQuestions().add(question);
    }

    public void onAddHeader(SurveyPage page) {
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.FULLWIDTH_HEADER);
        question.setOrder(page.getQuestions().size());
        question.setPageId(page.getId());
        page.getQuestions().add(question);
    }

    /**
     * @param direction set -1 to move upward or 1 to move down
     */
    public void onMoveQuestion(SurveyPage page, SurveyQuestion question, int direction) {
        int oldOrder = question.getOrder();
        question.setOrder(oldOrder + direction); // move selected question
        surveyDao.saveQuestion(question);

        SurveyQuestion neighbor = page.getQuestions().get(question.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        surveyDao.saveQuestion(neighbor);
        page.getQuestions().sort(Comparator.comparingInt(SurveyQuestion::getOrder));
    }

    public void onDeleteQuestion(SurveyPage page, SurveyQuestion question) {
        question.setDeleted(true);
        surveyDao.saveQuestion(question);
        page.getQuestions().remove(question);
    }

    public void onDuplicateQuestion(SurveyPage page, SurveyQuestion question) {
        SurveyQuestion duplicate = new SurveyQuestion(question);
        duplicate.setOrder(page.getQuestions().size());
        duplicate.setPageId(page.getId());
        surveyDao.saveQuestion(duplicate);
        page.getQuestions().add(duplicate);
    }

    public void onConditionQuestionChange(SurveyPage page) {
        page.setRequiredAnswer(null);
        surveyDao.savePage(page);
    }

    public void onConditionValueChange(SurveyPage page) {
        surveyDao.savePage(page);
    }

    public void onClearCondition(SurveyPage page) {
        page.setRequiredQuestionId(null);
        page.setRequiredAnswer(null);
        surveyDao.savePage(page);
    }
}
