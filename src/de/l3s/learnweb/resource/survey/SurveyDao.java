package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.RsHelper;
import de.l3s.util.SqlHelper;
import de.l3s.util.bean.BeanHelper;

@RegisterRowMapper(SurveyDao.SurveyMapper.class)
public interface SurveyDao extends SqlObject, Serializable {

    default Optional<SurveyResource> findResourceById(int surveyResourceId) {
        Resource resource = getHandle().attach(ResourceDao.class).findById(surveyResourceId);
        return convertToSurveyResource(resource);
    }

    default Optional<SurveyResource> convertToSurveyResource(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }

        if (resource.getType() != ResourceType.survey) {
            LogManager.getLogger(SurveyDao.class)
                .error("Survey resource requested but the resource is of type {}; {}", resource.getType(), BeanHelper.getRequestSummary());
            return Optional.empty();
        }

        return Optional.of((SurveyResource) resource);
    }

    @SqlQuery("SELECT *, (SELECT COUNT(*) FROM lw_survey_resource sr WHERE sr.survey_id = s.survey_id) as res_count FROM lw_survey s WHERE survey_id = ?")
    Optional<Survey> findById(int surveyId);

    @SqlQuery("SELECT *, (SELECT COUNT(*) FROM lw_survey_resource sr WHERE sr.survey_id = s.survey_id) as res_count FROM lw_survey s WHERE organization_id = ? and deleted = 0 and (public_template = 1 or user_id = ?)")
    List<Survey> findByOrganisationIdOrUserId(int organizationId, int userId);

    /**
     * @return true if the given survey was submitted by the user
     */
    @SqlQuery("SELECT submitted FROM lw_survey_resource_user WHERE resource_id = ? AND user_id = ?")
    Optional<Boolean> findSubmittedStatus(int resourceId, int userId);

    @SqlUpdate("INSERT INTO lw_survey_resource_user (resource_id, user_id, submitted) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE submitted = VALUES(submitted)")
    void insertSubmittedStatus(int resourceId, int userId, boolean submitted);

    @SqlUpdate("UPDATE lw_survey SET deleted = 1 WHERE survey_id = ?")
    void deleteSoft(Survey survey);

    /**
     * Returns all answers a user has given for a particular survey resource.
     */
    default SurveyUserAnswers findAnswersByResourceAndUserId(final SurveyResource resource, int userId) {
        Validate.notNull(resource);
        Validate.isTrue(userId > 0, "The value must be greater than zero: ", userId);
        Validate.isTrue(resource.getId() > 0, "The value must be greater than zero: ", resource.getId());

        // Get survey data
        SurveyUserAnswers surveyAnswer = getHandle().select("SELECT * FROM lw_survey_answer WHERE resource_id = ? AND user_id = ?", resource.getId(), userId)
            .reduceResultSet(new SurveyUserAnswers(userId, resource.getId()), (previous, rs, ctx) -> {
                previous.setSaved(true);

                SurveyQuestion question = resource.getSurvey().getQuestion(rs.getInt("question_id"));
                if (question != null) {
                    // distinguish questions with single and multiple answers
                    SurveyQuestion.QuestionType ansType = question.getType();

                    if (ansType == SurveyQuestion.QuestionType.MULTIPLE_MENU || ansType == SurveyQuestion.QuestionType.MANY_CHECKBOX) {
                        String[] answer = StringUtils.defaultString(rs.getString("answer")).split("\\s*\\|\\|\\|\\s*");

                        previous.getMultipleAnswers().put(rs.getInt("question_id"), answer);
                    } else {
                        previous.getAnswers().put(rs.getInt("question_id"), rs.getString("answer"));
                    }
                }

                return previous;
            });

        if (surveyAnswer.isSaved()) {
            surveyAnswer.setSubmitted(findSubmittedStatus(resource.getId(), userId).orElse(false));
        }

        return surveyAnswer;
    }

    @RegisterRowMapper(SurveyQuestionOptionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question_option WHERE question_id = ? and deleted = 0")
    List<SurveyQuestionOption> findAnswersByQuestionId(int questionId);

    @RegisterRowMapper(SurveyQuestionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question WHERE survey_id = ? and deleted = 0 ORDER BY `order`")
    List<SurveyQuestion> findQuestionsBySurveyId(int surveyId);

    default List<SurveyQuestion> findQuestionsAndAnswersById(int surveyId) {
        List<SurveyQuestion> questions = new ArrayList<>();

        if (surveyId <= 0) {
            return questions;
        }

        return findQuestionsBySurveyId(surveyId).stream()
            .peek(question -> question.getAnswers().addAll(findAnswersByQuestionId(question.getId())))
            .collect(Collectors.toList());
    }

    /**
     * Loads the survey metadata into the given SurveyResource.
     */
    default void loadSurveyResource(SurveyResource resource) {
        getHandle().select("SELECT * FROM lw_survey_resource WHERE resource_id = ?", resource.getId()).map((rs, ctx) -> {
            resource.setEnd(RsHelper.getLocalDateTime(rs.getTimestamp("close_date")));
            resource.setStart(RsHelper.getLocalDateTime(rs.getTimestamp("open_date")));
            resource.setSurveyId(rs.getInt("survey_id"));
            resource.setSaveable(rs.getBoolean("editable"));
            return resource;
        });
    }

    /**
     * Persists the survey. Updates ids if not yet stored.
     *
     * @param updateMetadataOnly performance optimization: if true only metadata like title and description will be saved but not changes to questions
     */
    default void save(Survey survey, boolean updateMetadataOnly) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("survey_id", survey.getId() < 1 ? null : survey.getId());
        params.put("organization_id", survey.getOrganizationId());
        params.put("title", survey.getTitle());
        params.put("description", survey.getDescription());
        params.put("user_id", survey.getUserId());
        params.put("public_template", survey.isPublicTemplate());

        Optional<Integer> surveyId = SqlHelper.handleSave(getHandle(), "lw_survey", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        surveyId.ifPresent(survey::setId);

        if (!updateMetadataOnly) {
            for (SurveyQuestion question : survey.getQuestions()) {
                question.setSurveyId(survey.getId()); // need to update id in case survey has just been created
                saveQuestion(question);
            }
        }
    }

    default void saveQuestion(SurveyQuestion question) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("question_id", question.getId() < 1 ? null : question.getId());
        params.put("survey_id", question.getSurveyId());
        params.put("deleted", question.isDeleted());
        params.put("order", question.getOrder());
        params.put("question", question.getLabel());
        params.put("question_type", question.getType());
        params.put("option", SurveyQuestion.joinOptions(question.getOptions()));
        params.put("info", question.getInfo());
        params.put("required", question.isRequired());

        Optional<Integer> questionId = SqlHelper.handleSave(getHandle(), "lw_survey_question", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        questionId.ifPresent(question::setId);

        for (SurveyQuestionOption answer : question.getAnswers()) {
            saveQuestionOption(question.getId(), answer);
        }
    }

    default void saveQuestionOption(int questionId, SurveyQuestionOption option) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("answer_id", option.getId() < 1 ? null : option.getId());
        params.put("deleted", option.isDeleted());
        params.put("question_id", questionId);
        params.put("value", option.getValue());

        Optional<Integer> optionId = SqlHelper.handleSave(getHandle(), "lw_survey_question_option", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        optionId.ifPresent(option::setId);
    }

    default void saveSurveyResource(SurveyResource surveyResource) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_id", surveyResource.getId());
        params.put("survey_id", surveyResource.getSurveyId());
        params.put("open_date", surveyResource.getStart());
        params.put("close_date", surveyResource.getEnd());
        params.put("editable", surveyResource.isSaveable());

        SqlHelper.handleSave(getHandle(), "lw_survey_resource", params).execute();
    }

    /**
     * @param finalSubmit true if this is the final submit
     */
    default void saveAnswers(SurveyUserAnswers surveyAnswer, final boolean finalSubmit) {
        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_survey_answer (resource_id, user_id, question_id, answer) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE answer = VALUES(answer)");

        surveyAnswer.getAnswers().forEach((questionId, answer) ->
            batch.add(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), questionId, answer));

        surveyAnswer.getMultipleAnswers().forEach((questionId, answer) ->
            batch.add(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), questionId, SurveyUserAnswers.joinMultipleAnswers(answer)));

        batch.execute();
        surveyAnswer.setSaved(true);

        if (finalSubmit) {
            insertSubmittedStatus(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), true);
            surveyAnswer.setSubmitted(true);
        }
    }

    class SurveyMapper implements RowMapper<Survey> {
        @Override
        public Survey map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Survey survey = new Survey();
            survey.setId(rs.getInt("survey_id"));
            survey.setTitle(rs.getString("title"));
            survey.setDescription(rs.getString("description"));
            survey.setOrganizationId(rs.getInt("organization_id"));
            survey.setUserId(rs.getInt("user_id"));
            survey.setDeleted(rs.getBoolean("deleted"));
            survey.setPublicTemplate(rs.getBoolean("public_template"));
            survey.setAssociated(rs.getInt("res_count") > 0);
            return survey;
        }
    }

    class SurveyQuestionMapper implements RowMapper<SurveyQuestion> {
        @Override
        public SurveyQuestion map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.valueOf(rs.getString("question_type")));
            question.setId(rs.getInt("question_id"));
            question.setSurveyId(rs.getInt("survey_id"));
            question.setLabel(rs.getString("question"));
            question.setInfo(rs.getString("info"));
            question.setRequired(rs.getBoolean("required"));
            question.setOrder(rs.getInt("order"));

            String options = rs.getString("option");
            if (!StringUtils.isEmpty(options)) {
                String[] optionsArray = options.trim().split("\\s*\\|\\|\\|\\s*");

                if (optionsArray.length == 2) {
                    question.getOptions().put("minLength", optionsArray[0]);
                    question.getOptions().put("maxLength", optionsArray[1]);
                }
            }

            return question;
        }
    }

    class SurveyQuestionOptionMapper implements RowMapper<SurveyQuestionOption> {
        @Override
        public SurveyQuestionOption map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyQuestionOption answer = new SurveyQuestionOption();
            answer.setValue(rs.getString("value"));
            answer.setId(rs.getInt("answer_id"));
            answer.setDeleted(rs.getBoolean("deleted"));
            return answer;
        }
    }
}
