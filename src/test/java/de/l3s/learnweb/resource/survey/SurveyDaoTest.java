package de.l3s.learnweb.resource.survey;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
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
        assertEquals(1, surveyResource.get().getSurveyId());
    }

    @Test
    void convertToSurveyResource() {
        Optional<Resource> resource = surveyDao.getResourceDao().findById(10);
        assertTrue(resource.isPresent());
        Optional<SurveyResource> surveyResource = surveyDao.convertToSurveyResource(resource.get());
        assertTrue(surveyResource.isPresent());
        assertEquals(1, surveyResource.get().getSurveyId());
    }

    @Test
    void findById() {
        Optional<Survey> survey = surveyDao.findById(1);
        assertTrue(survey.isPresent());
        assertEquals(1, survey.get().getId());
    }

    @Test
    void findByOrganisationIdOrUserId() {
        List<Survey> surveyList = surveyDao.findByOrganisationIdOrUserId(1, 1);
        assertFalse(surveyList.isEmpty());
        assertArrayEquals(new Integer[] {1}, surveyList.stream().map(Survey::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findSubmittedStatus() {
        Optional<Boolean> submittedStatus = surveyDao.findSubmittedStatus(10, 2);
        assertTrue(submittedStatus.isPresent());
        assertTrue(submittedStatus.get());
    }

    @Test
    void insertSubmittedStatus() {
        Optional<Boolean> submittedStatus = surveyDao.findSubmittedStatus(10, 2);
        assertTrue(submittedStatus.isPresent());
        assertTrue(submittedStatus.get());
        surveyDao.insertSubmittedStatus(10, 2, false);
        submittedStatus = surveyDao.findSubmittedStatus(10, 2);
        assertTrue(submittedStatus.isPresent());
        assertFalse(submittedStatus.get());
    }

    @Test
    void deleteSoft() {
        Optional<Survey> survey = surveyDao.findById(1);
        assertTrue(survey.isPresent());
        assertFalse(survey.get().isDeleted());
        surveyDao.deleteSoft(survey.get());
        survey = surveyDao.findById(1);
        assertTrue(survey.isPresent());
        assertTrue(survey.get().isDeleted());
    }

    @Test
    void findAnswersByResourceAndUserId() {
        Optional<SurveyResource> resource = surveyDao.findResourceById(10);
        assertTrue(resource.isPresent());
        SurveyUserAnswers answers = surveyDao.findAnswersByResourceAndUserId(resource.get(), 2);
        assertEquals(2, answers.getId());
    }

    @Test
    void findAnswersByQuestionId() {
        List<SurveyQuestionOption> surveyQuestionOptions = surveyDao.findAnswersByQuestionId(2);
        assertFalse(surveyQuestionOptions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3}, surveyQuestionOptions.stream().map(SurveyQuestionOption::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findQuestionsBySurveyId() {
        List<SurveyQuestion> surveyQuestions = surveyDao.findQuestionsBySurveyId(1);
        assertFalse(surveyQuestions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, surveyQuestions.stream().map(SurveyQuestion::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findQuestionsAndAnswersById() {
        List<SurveyQuestion> surveyQuestions = surveyDao.findQuestionsAndAnswersById(1);
        assertFalse(surveyQuestions.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, surveyQuestions.stream().map(SurveyQuestion::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void loadSurveyResource() {
        Optional<SurveyResource> surveyResource = surveyDao.findResourceById(10);
        assertTrue(surveyResource.isPresent());
        surveyDao.loadSurveyResource(surveyResource.get());
    }

    @Test
    void save() {
        Survey survey = new Survey();
        survey.setId(4);
        survey.setOrganisationId(1);
        survey.setTitle("ABC");
        survey.setDescription("test");
        survey.setUserId(1);
        survey.setPublicTemplate(true);
        surveyDao.save(survey, true);
        Optional<Survey> retrieved = surveyDao.findById(4);
        assertTrue(retrieved.isPresent());
        assertEquals("ABC", retrieved.get().getTitle());
    }

    @Test
    void saveQuestion() {
        List<SurveyQuestion> before = surveyDao.findQuestionsBySurveyId(1);
        assertFalse(before.isEmpty());
        SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.AUTOCOMPLETE);
        question.setId(11);
        question.setSurveyId(1);
        question.setDeleted(false);
        question.setOrder(8);
        question.setLabel("Question ten");
        question.setInfo("");
        question.setRequired(true);
        surveyDao.saveQuestion(question);
        List<SurveyQuestion> retrieved = surveyDao.findQuestionsBySurveyId(1);
        assertFalse(retrieved.isEmpty());
        assertNotEquals(before.size(), retrieved.stream().map(SurveyQuestion::getId).count());
    }

    @Test
    void saveQuestionOption() {
        List<SurveyQuestionOption> before = surveyDao.findAnswersByQuestionId(1);
        assertTrue(before.isEmpty());
        SurveyQuestionOption surveyQuestionOption = new SurveyQuestionOption();
        surveyDao.saveQuestionOption(1, surveyQuestionOption);
        List<SurveyQuestionOption> retrieved = surveyDao.findAnswersByQuestionId(1);
        assertFalse(retrieved.isEmpty());
    }

    @Test
    void saveSurveyResource() {
        SurveyResource surveyResource = new SurveyResource();
        surveyResource.setId(3);
        surveyResource.setSurveyId(1);
        surveyResource.setEnd(LocalDateTime.now());
        surveyResource.setSaveable(false);
        surveyResource.setStart(LocalDateTime.of(2020, 1, 1, 8, 0));
        surveyDao.saveSurveyResource(surveyResource);

        Optional<SurveyResource> retrieved = surveyDao.findResourceById(10);
        assertTrue(retrieved.isPresent());
        assertEquals(1, retrieved.get().getSurveyId());
    }

    @Test
    void saveAnswers() {
        SurveyUserAnswers surveyUserAnswers = new SurveyUserAnswers(3, 10);
        surveyUserAnswers.setSubmitted(true);
        surveyDao.saveAnswers(surveyUserAnswers, true);
        Optional<SurveyResource> surveyResource = surveyDao.findResourceById(10);
        assertTrue(surveyResource.isPresent());
        SurveyUserAnswers retrieved = surveyDao.findAnswersByResourceAndUserId(surveyResource.get(), 3);
        assertEquals(3, retrieved.getId());
    }
}
