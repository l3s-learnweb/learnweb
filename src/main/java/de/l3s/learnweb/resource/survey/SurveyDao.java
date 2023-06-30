package de.l3s.learnweb.resource.survey;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.Cache;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

public interface SurveyDao extends SqlObject, Serializable {
    ICache<SurveyResponse> responseCache = new Cache<>(500);

    @CreateSqlObject
    ResourceDao getResourceDao();

    default Optional<SurveyResource> findResourceById(int surveyResourceId) {
        return convertToSurveyResource(getResourceDao().findById(surveyResourceId).orElse(null));
    }

    default SurveyResource findResourceByIdOrElseThrow(int surveyResourceId) {
        return convertToSurveyResource(getResourceDao().findById(surveyResourceId).orElse(null))
            .orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_object_description"));
    }

    default List<SurveyResource> findByUserId(int userId) {
        return getResourceDao().findByOwnerIdsAndType(Collections.singleton(userId), ResourceType.survey)
            .stream().map(this::convertToSurveyResource).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    default Optional<SurveyResource> convertToSurveyResource(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }

        if (resource instanceof SurveyResource survey) {
            return Optional.of(survey);
        } else {
            LogManager.getLogger(SurveyDao.class).error("Survey resource requested but the resource is of type {}", resource.getType());
            return Optional.empty();
        }
    }

    @RegisterRowMapper(SurveyQuestionOptionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question_option WHERE question_id = ? and deleted = 0")
    List<SurveyQuestionOption> findOptionsByQuestionId(int questionId);

    @RegisterRowMapper(SurveyPageVariantMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_page_variant WHERE page_id = ?")
    List<SurveyPageVariant> findVariantsByPageId(int pageId);

    @RegisterRowMapper(SurveyQuestionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question WHERE question_id = ? and deleted = 0")
    Optional<SurveyQuestion> findQuestionById(int questionId);

    @RegisterRowMapper(SurveyQuestionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question WHERE resource_id = ? and deleted = 0 ORDER BY `order`")
    List<SurveyQuestion> findQuestionsByResourceId(int resourceId);

    @RegisterRowMapper(SurveyQuestionMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_question WHERE resource_id = ? and page_id = ? and deleted = 0 ORDER BY `order`")
    List<SurveyQuestion> findQuestionsByResourceIdAndPageId(int resourceId, int pageId);

    @RegisterRowMapper(SurveyPageMapper.class)
    @SqlQuery("SELECT * FROM lw_survey_page WHERE resource_id = ? and deleted = 0 ORDER BY `order`")
    List<SurveyPage> findPagesByResourceId(int resourceId);

    default List<SurveyPage> findPagesAndVariantsByResourceId(int resourceId) {
        return findPagesByResourceId(resourceId).stream()
            .peek(page -> page.getVariants().addAll(findVariantsByPageId(page.getId())))
            .collect(Collectors.toList());
    }

    default List<SurveyQuestion> findQuestionsAndOptionsByResourceId(int resourceId) {
        return findQuestionsByResourceId(resourceId).stream()
            .peek(question -> question.getOptions().addAll(findOptionsByQuestionId(question.getId())))
            .collect(Collectors.toList());
    }

    default List<SurveyQuestion> findQuestionsAndOptionsByResourceId(int resourceId, int pageId) {
        return findQuestionsByResourceIdAndPageId(resourceId, pageId).stream()
            .peek(question -> question.getOptions().addAll(findOptionsByQuestionId(question.getId())))
            .collect(Collectors.toList());
    }

    /**
     * Persists the survey. Updates ids if not yet stored.
     */
    default void savePages(SurveyResource resource) {
        for (SurveyPage page : resource.getPages()) {
            page.setResourceId(resource.getId()); // need to update id in case survey has just been created
            savePage(page);
            saveQuestions(page);
        }
    }

    /**
     * Persists the survey. Updates ids if not yet stored.
     */
    default void saveQuestions(SurveyPage page) {
        for (SurveyQuestion question : page.getQuestions()) {
            question.setResourceId(page.getResourceId()); // need to update id in case survey has just been created
            question.setPageId(page.getId());
            saveQuestion(question);
        }
    }

    default void savePage(SurveyPage page) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_id", page.getResourceId());
        params.put("page_id", SqlHelper.toNullable(page.getId()));
        params.put("deleted", page.isDeleted());
        params.put("order", page.getOrder());
        params.put("title", SqlHelper.toNullable(page.getTitle()));
        params.put("description", SqlHelper.toNullable(page.getDescription()));
        params.put("sampling", page.isSampling());

        Optional<Integer> pageId = SqlHelper.handleSave(getHandle(), "lw_survey_page", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (pageId.isPresent() && pageId.get() != 0) {
            page.setId(pageId.get());
        }
    }

    default void saveQuestion(SurveyQuestion question) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("resource_id", question.getResourceId());
        params.put("question_id", SqlHelper.toNullable(question.getId()));
        params.put("page_id", SqlHelper.toNullable(question.getPageId()));
        params.put("deleted", question.isDeleted());
        params.put("order", question.getOrder());
        params.put("question_type", question.getType());
        params.put("question", StringUtils.defaultString(question.getQuestion()));
        params.put("description", question.getDescription());
        params.put("required", question.isRequired());
        params.put("min_length", question.getMinLength());
        params.put("max_length", question.getMaxLength());

        Optional<Integer> questionId = SqlHelper.handleSave(getHandle(), "lw_survey_question", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (questionId.isPresent() && questionId.get() != 0) {
            question.setId(questionId.get());
        }

        for (SurveyQuestionOption answer : question.getOptions()) {
            saveQuestionOption(question.getId(), answer);
        }
    }

    default void saveQuestionOption(int questionId, SurveyQuestionOption option) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("question_id", questionId);
        params.put("option_id", SqlHelper.toNullable(option.getId()));
        params.put("deleted", option.isDeleted());
        params.put("value", option.getValue());

        Optional<Integer> optionId = SqlHelper.handleSave(getHandle(), "lw_survey_question_option", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (optionId.isPresent() && optionId.get() != 0) {
            option.setId(optionId.get());
        }
    }

    /**
     * @return {@code true} if the given survey was submitted by the user
     */
    @SqlQuery("SELECT submitted FROM lw_survey_response WHERE resource_id = ? AND user_id = ?")
    Optional<Boolean> findSubmittedStatus(int resourceId, int userId);

    /**
     * Returns all answers a user has given for a particular survey resource.
     */
    default SurveyResponse findResponseByResourceAndUserId(final int resourceId, final int userId) {
        Optional<SurveyResponse> select = getHandle().select("SELECT * FROM lw_survey_response WHERE resource_id = ? AND user_id = ? LIMIT 1", resourceId, userId)
            .registerRowMapper(new SurveyResponseMapper()).mapTo(SurveyResponse.class).findOne();

        return select.map(this::findAnswersByResponse).orElse(null);
    }

    default SurveyResponse findResponseById(final int responseId) {
        SurveyResponse response = responseCache.get(responseId);
        if (response == null) {
            Optional<SurveyResponse> select = getHandle().select("SELECT * FROM lw_survey_response WHERE response_id = ?", responseId)
                .registerRowMapper(new SurveyResponseMapper()).mapTo(SurveyResponse.class).findOne();

            if (select.isPresent()) {
                return findAnswersByResponse(select.get());
            }
        }
        return response;
    }

    default List<SurveyResponse> findResponsesByResourceId(final int resourceId) {
        return getHandle().select("SELECT * FROM lw_survey_response WHERE resource_id = ?", resourceId)
            .registerRowMapper(new SurveyResponseMapper()).mapTo(SurveyResponse.class)
            .map(this::findAnswersByResponse).collect(Collectors.toList());
    }

    default List<SurveyResponse> findSubmittedResponsesByResourceId(final int resourceId) {
        return getHandle().select("SELECT * FROM lw_survey_response WHERE resource_id = ? AND submitted = 1", resourceId)
            .registerRowMapper(new SurveyResponseMapper()).mapTo(SurveyResponse.class)
            .map(this::findAnswersByResponse).collect(Collectors.toList());
    }

    default List<SurveyResponse> findResponsesByUserId(final int resourceId) {
        return getHandle().select("SELECT * FROM lw_survey_response WHERE user_id = ?", resourceId)
            .registerRowMapper(new SurveyResponseMapper()).mapTo(SurveyResponse.class)
            .map(this::findAnswersByResponse).collect(Collectors.toList());
    }

    default SurveyResponse findAnswersByResponse(final SurveyResponse response) {
        return getHandle().select("SELECT * FROM lw_survey_response_answer WHERE response_id = ?", response.getId())
            .reduceResultSet(response, (previous, rs, ctx) -> {
                int questionId = rs.getInt("question_id");
                String answer = rs.getString("answer");

                Optional<SurveyQuestion> question = findQuestionById(questionId);
                if (question.isPresent()) {
                    if (question.get().getType().isMultiple()) {
                        previous.getMultipleAnswers().put(questionId, SurveyResponse.splitAnswers(answer));
                    } else {
                        previous.getAnswers().put(questionId, answer);
                    }
                }
                return previous;
            });
    }

    default void saveResponse(SurveyResponse response) {
        if (response.getCreatedAt() == null) {
            response.setCreatedAt(SqlHelper.now());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("response_id", SqlHelper.toNullable(response.getId()));
        params.put("resource_id", response.getResourceId());
        params.put("user_id", SqlHelper.toNullable(response.getUserId()));
        params.put("submitted", response.isSubmitted());
        params.put("created_at", response.getCreatedAt());

        Optional<Integer> responseId = SqlHelper.handleSave(getHandle(), "lw_survey_response", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (responseId.isPresent() && responseId.get() != 0) {
            response.setId(responseId.get());
            responseCache.put(response);
        }

        saveAnswers(response);
    }

    default void saveAnswers(SurveyResponse response) {
        PreparedBatch batch = getHandle().prepareBatch("INSERT INTO lw_survey_response_answer (response_id, question_id, answer) VALUES (?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE answer = VALUES(answer)");

        response.getAnswers().forEach((questionId, answer) ->
            batch.add(response.getId(), questionId, answer));

        response.getMultipleAnswers().forEach((questionId, answer) ->
            batch.add(response.getId(), questionId, SurveyResponse.joinAnswers(answer)));

        batch.execute();
    }

    default void saveAnswer(SurveyResponse response, int questionId, int variantId, String answer) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("response_id", response.getId());
        params.put("question_id", questionId);
        params.put("variant_id", SqlHelper.toNullable(variantId));
        params.put("answer", answer);

        SqlHelper.handleSave(getHandle(), "lw_survey_response_answer", params).execute();
    }

    class SurveyPageMapper implements RowMapper<SurveyPage> {
        @Override
        public SurveyPage map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyPage page = new SurveyPage(rs.getInt("resource_id"));
            page.setId(rs.getInt("page_id"));
            page.setDeleted(rs.getBoolean("deleted"));
            page.setOrder(rs.getInt("order"));
            page.setTitle(rs.getString("title"));
            page.setDescription(rs.getString("description"));
            page.setSampling(rs.getBoolean("sampling"));
            return page;
        }
    }

    class SurveyPageVariantMapper implements RowMapper<SurveyPageVariant> {
        @Override
        public SurveyPageVariant map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyPageVariant variant = new SurveyPageVariant();
            variant.setId(rs.getInt("variant_id"));
            variant.setDescription(rs.getString("description"));
            return variant;
        }
    }

    class SurveyQuestionMapper implements RowMapper<SurveyQuestion> {
        @Override
        public SurveyQuestion map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyQuestion question = new SurveyQuestion(SurveyQuestion.QuestionType.valueOf(rs.getString("question_type")), rs.getInt("resource_id"));
            question.setId(rs.getInt("question_id"));
            question.setPageId(rs.getInt("page_id"));
            question.setDeleted(rs.getBoolean("deleted"));
            question.setOrder(rs.getInt("order"));
            question.setQuestion(rs.getString("question"));
            question.setDescription(rs.getString("description"));
            question.setRequired(rs.getBoolean("required"));
            question.setMinLength(SqlHelper.toNullable(rs.getInt("min_length")));
            question.setMaxLength(SqlHelper.toNullable(rs.getInt("max_length")));
            return question;
        }
    }

    class SurveyQuestionOptionMapper implements RowMapper<SurveyQuestionOption> {
        @Override
        public SurveyQuestionOption map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyQuestionOption answer = new SurveyQuestionOption(rs.getInt("question_id"));
            answer.setId(rs.getInt("option_id"));
            answer.setDeleted(rs.getBoolean("deleted"));
            answer.setValue(rs.getString("value"));
            return answer;
        }
    }

    class SurveyResponseMapper implements RowMapper<SurveyResponse> {
        @Override
        public SurveyResponse map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            SurveyResponse response = responseCache.get(rs.getInt("response_id"));

            if (response == null) {
                response = new SurveyResponse(rs.getInt("resource_id"));
                response.setId(rs.getInt("response_id"));
                response.setUserId(rs.getInt("user_id"));
                response.setSubmitted(rs.getBoolean("submitted"));
                response.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
                responseCache.put(response);
            }
            return response;
        }
    }
}
