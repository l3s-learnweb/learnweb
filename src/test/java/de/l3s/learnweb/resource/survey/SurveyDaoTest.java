package de.l3s.learnweb.resource.survey;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.resource.Resource;
import de.l3s.test.LearnwebExtension;

class SurveyDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final SurveyDao surveyDao = learnwebExt.attach(SurveyDao.class);

    @Test
    void findResourceById() {
        Optional<SurveyResource> surveyResource = surveyDao.findResourceById(10);
        assertTrue(surveyResource.isPresent());
        assertEquals(10, surveyResource.get().getId());
    }

    @Test
    void convertToSurveyResource() {
        Optional<Resource> resource = surveyDao.getResourceDao().findById(10);
        assertTrue(resource.isPresent());
        Optional<SurveyResource> surveyResource = surveyDao.convertToSurveyResource(resource.get());
        assertTrue(surveyResource.isPresent());
    }

    @Test
    void findSubmittedStatus() {
        Optional<Boolean> submittedStatus = surveyDao.findSubmittedStatus(10, 2);
        assertTrue(submittedStatus.isPresent());
        assertTrue(submittedStatus.get());
    }

    @Test
    void findOptionsByQuestionId() {
        List<SurveyQuestionOption> surveyQuestionOptions = surveyDao.findOptionsByQuestionId(2);
        assertFalse(surveyQuestionOptions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3}, surveyQuestionOptions.stream().map(SurveyQuestionOption::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findQuestionsBySurveyId() {
        List<SurveyQuestion> surveyQuestions = surveyDao.findQuestionsByResourceId(10);
        assertFalse(surveyQuestions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, surveyQuestions.stream().map(SurveyQuestion::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findQuestionsAndAnswersById() {
        List<SurveyQuestion> surveyQuestions = surveyDao.findQuestionsAndOptionsByResourceId(10);
        assertFalse(surveyQuestions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, surveyQuestions.stream().map(SurveyQuestion::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void saveQuestion() {
        List<SurveyQuestion> before = surveyDao.findQuestionsByResourceId(10);
        assertFalse(before.isEmpty());

        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.MANY_CHECKBOX);
        question.setId(11);
        question.setPageId(1);
        question.setDeleted(false);
        question.setOrder(8);
        question.setQuestion("Question ten");
        question.setDescription("");
        question.setRequired(true);
        surveyDao.saveQuestion(question);

        List<SurveyQuestion> retrieved = surveyDao.findQuestionsByResourceId(10);
        assertFalse(retrieved.isEmpty());
        assertNotEquals(before.size(), retrieved.stream().map(SurveyQuestion::getId).count());
    }

    @Test
    void saveQuestionOption() {
        List<SurveyQuestionOption> before = surveyDao.findOptionsByQuestionId(1);
        assertTrue(before.isEmpty());
        SurveyQuestionOption surveyQuestionOption = new SurveyQuestionOption();
        surveyDao.saveQuestionOption(1, surveyQuestionOption);
        List<SurveyQuestionOption> retrieved = surveyDao.findOptionsByQuestionId(1);
        assertFalse(retrieved.isEmpty());
    }

    @Test
    void saveResponse() {
        SurveyResponse response = new SurveyResponse();
        response.setResourceId(10);
        response.setUserId(3);
        response.setSubmitted(true);
        surveyDao.saveResponse(response);

        Optional<SurveyResource> surveyResource = surveyDao.findResourceById(10);
        assertTrue(surveyResource.isPresent());

        SurveyResponse retrieved = surveyDao.findResponseById(response.getId());
        assertEquals(3, retrieved.getUserId());
    }
}
