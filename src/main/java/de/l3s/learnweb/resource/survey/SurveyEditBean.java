package de.l3s.learnweb.resource.survey;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class SurveyEditBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 669288862248912801L;
    private static final Logger log = LogManager.getLogger(SurveyEditBean.class);

    private SurveyResource resource;
    private ArrayList<SelectItem> questionTypes;

    @Inject
    private SurveyDao surveyDao;

    @PostConstruct
    public void onLoad() {
        Resource baseResource = Beans.getInstance(ResourceDetailBean.class).getResource();
        resource = surveyDao.convertToSurveyResource(baseResource).orElseThrow(BeanAssert.NOT_FOUND);
        BeanAssert.hasPermission(resource.canModerateResource(getUser()));
    }

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
                SurveyQuestion.QuestionType.ONE_RADIO,
                SurveyQuestion.QuestionType.MANY_CHECKBOX,
                SurveyQuestion.QuestionType.ONE_MENU,
                SurveyQuestion.QuestionType.ONE_MENU_EDITABLE,
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
        question.getOptions().add(new SurveyQuestionOption(question.getId()));
    }

    public void onQuestionOptionChange(SurveyQuestion question, SurveyQuestionOption answer) {
        // log.info("New value: [{}] {}", question.getId(), answer);
        surveyDao.saveQuestionOption(question.getId(), answer);
    }

    public void onQuestionOptionDelete(SurveyQuestion question, SurveyQuestionOption answer) {
        answer.setDeleted(true);
        surveyDao.saveQuestionOption(question.getId(), answer);
    }

    public void onAddQuestion(SurveyPage page) {
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.INPUT_TEXT, resource.getId());
        question.setOrder(page.getQuestions().size());
        question.setPageId(page.getId());
        page.getQuestions().add(question);
    }

    public void onAddHeader(SurveyPage page) {
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.FULLWIDTH_HEADER, resource.getId());
        question.setOrder(page.getQuestions().size());
        question.setPageId(page.getId());
        page.getQuestions().add(question);
    }

    public void onAddPage() {
        List<SurveyPage> pages = resource.getPages();
        SurveyPage page = new SurveyPage(resource.getId());
        page.setOrder(pages.size());
        surveyDao.savePage(page);
        pages.add(page);
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

    /**
     * @param direction set -1 to move upward or 1 to move down
     */
    public void onMovePage(SurveyPage page, int direction) {
        int oldOrder = page.getOrder();
        page.setOrder(oldOrder + direction); // move selected question
        surveyDao.savePage(page);

        SurveyPage neighbor = resource.getPages().get(page.getOrder());
        neighbor.setOrder(oldOrder); // move neighbor question
        surveyDao.savePage(neighbor);
        resource.getPages().sort(Comparator.comparingInt(SurveyPage::getOrder));
    }

    public void onDeleteQuestion(SurveyPage page, SurveyQuestion question) {
        question.setDeleted(true);
        surveyDao.saveQuestion(question);
        page.getQuestions().remove(question);
    }

    public List<SurveyPage> getPages() {
        return resource.getPages().stream().filter(page -> !page.isDeleted()).toList();
    }
}
