package de.l3s.learnweb.resource.survey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.survey.SurveyQuestion.QuestionType;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.bean.BeanHelper;
import de.l3s.util.SqlHelper;

/**
 * @author Philipp Kemkes
 */
public class SurveyManager {
    private static final Logger log = LogManager.getLogger(SurveyManager.class);
    private final Learnweb learnweb;

    public SurveyManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    /**
     * Returns all answers a user has given for a particular survey resource.
     */
    protected SurveyUserAnswers getAnswersOfUser(final SurveyResource surveyResource, int userId) throws SQLException {
        Validate.notNull(surveyResource);
        Validate.isTrue(userId > 0, "The value must be greater than zero: ", userId);
        Validate.isTrue(surveyResource.getId() > 0, "The value must be greater than zero: ", surveyResource.getId());

        SurveyUserAnswers surveyAnswer = new SurveyUserAnswers(userId, surveyResource.getId());

        //Get survey data
        try (PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?")) {
            preparedStatement.setInt(1, surveyResource.getId());
            preparedStatement.setInt(2, userId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                surveyAnswer.setSaved(true);

                SurveyQuestion question = surveyResource.getSurvey().getQuestion(rs.getInt("question_id"));
                if (question != null) {
                    // distinguish questions with single and multiple answers
                    QuestionType ansType = question.getType();

                    if (ansType == QuestionType.MULTIPLE_MENU || ansType == QuestionType.MANY_CHECKBOX) {
                        String[] answer = StringUtils.defaultString(rs.getString("answer")).split("\\s*\\|\\|\\|\\s*");

                        surveyAnswer.getMultipleAnswers().put(rs.getInt("question_id"), answer);
                    } else {
                        surveyAnswer.getAnswers().put(rs.getInt("question_id"), rs.getString("answer"));
                    }
                }
            }
        }
        if (surveyAnswer.isSaved()) {
            surveyAnswer.setSubmitted(this.getSurveyResourceSubmitStatus(surveyResource.getId(), userId));
        }

        return surveyAnswer;
    }

    /**
     * loads the survey metadata into the given SurveyResource.
     */
    protected void loadSurveyResource(SurveyResource resource) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?")) {
            ps.setInt(1, resource.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                resource.setEnd(rs.getDate("close_date"));
                resource.setStart(rs.getDate("open_date"));
                resource.setSurveyId(rs.getInt("survey_id"));
                resource.setSaveable(rs.getBoolean("editable"));
            } else {
                log.error("Can't load metadata of survey resource: " + resource.getId());
            }
        }
    }

    public SurveyResource getSurveyResource(int surveyResourceId) throws SQLException {
        Resource resource = learnweb.getResourceManager().getResource(surveyResourceId);

        if (resource == null) {
            return null;
        }

        if (resource.getType() != ResourceType.survey) {
            log.error("Survey resource requested but the resource is of type {}; {}", resource.getType(), BeanHelper.getRequestSummary());
            return null;
        }

        return (SurveyResource) resource;
    }

    public Survey getSurvey(int surveyId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey` WHERE `survey_id` = ?")) {
            select.setInt(1, surveyId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return createSurvey(rs);
            } else {
                log.warn("Can't get survey: " + surveyId);
                return null;
            }
        }
    }

    private Survey createSurvey(ResultSet rs) throws SQLException {
        Survey survey = new Survey();

        survey.setId(rs.getInt("survey_id"));
        survey.setTitle(rs.getString("title"));
        survey.setDescription(rs.getString("description"));
        survey.setOrganizationId(rs.getInt("organization_id"));
        survey.setUserId(rs.getInt("user_id"));
        survey.setDeleted(rs.getInt("deleted") == 1);
        survey.setPublicTemplate(rs.getInt("public_template") == 1);
        survey.setAssociated(isSurveyAssociatedWithResource(rs.getInt("survey_id")));

        return survey;
    }

    public List<SurveyQuestion> getQuestions(int surveyId) throws SQLException {
        List<SurveyQuestion> questions = new ArrayList<>();

        if (surveyId <= 0) {
            return questions;
        }

        // load survey questions
        try (PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted` = 0 ORDER BY `order`")) {
            int previousQuestionOrder = -1; // used to check the integrity of order attribute
            boolean isOrderContinuous = true; // when false, ignore following errors for this survey

            preparedStatement.setInt(1, surveyId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                SurveyQuestion question = new SurveyQuestion(QuestionType.valueOf(rs.getString("question_type")));
                question.setId(rs.getInt("question_id"));
                question.setSurveyId(rs.getInt("survey_id"));
                question.setLabel(rs.getString("question"));
                question.setInfo(rs.getString("info"));
                question.setRequired(rs.getBoolean("required"));
                question.setOrder(rs.getInt("order"));

                if (previousQuestionOrder >= question.getOrder() && isOrderContinuous) {
                    log.error("Survey questions aren't ordered correctly. Survey_id: {}; Previous order: {}; Found order: {}", surveyId, previousQuestionOrder, question.getOrder());
                    isOrderContinuous = false;
                }

                String options = rs.getString("option");
                if (!StringUtils.isEmpty(options)) {
                    String[] optionsArray = options.trim().split("\\s*\\|\\|\\|\\s*");

                    if (optionsArray.length == 2) {
                        question.getOptions().put("minLength", optionsArray[0]);
                        question.getOptions().put("maxLength", optionsArray[1]);
                    }
                }

                try (PreparedStatement getAnswer = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_question_option` WHERE `question_id` = ? and `deleted` = 0")) {
                    getAnswer.setInt(1, question.getId());
                    ResultSet rsAnswer = getAnswer.executeQuery();
                    while (rsAnswer.next()) {
                        SurveyQuestionOption answer = new SurveyQuestionOption();
                        answer.setValue(rsAnswer.getString("value"));
                        answer.setId(rsAnswer.getInt("answer_id"));
                        answer.setDeleted(rsAnswer.getBoolean("deleted"));
                        question.getAnswers().add(answer);
                    }
                }

                questions.add(question);
            }
        }
        return questions;
    }

    /**
     * @param submitted true if the given user has finally submitted @param resourceId
     */
    private void setSurveyResourceSubmitStatus(int resourceId, int userId, boolean submitted) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO lw_survey_resource_user (`resource_id`, `user_id`, `submitted`) VALUES (?,?,?)")) {
            ps.setInt(1, resourceId);
            ps.setInt(2, userId);
            ps.setBoolean(3, submitted);
            int affectedRows = ps.executeUpdate();

            if (affectedRows != 1 && affectedRows != 2) { // 1 = inserted ; 2 = updated
                log.error(affectedRows + "; Did not store survey submit: " + ps);
            }
        }
    }

    /**
     * @return true if the given survey was submitted by the user
     */
    protected boolean getSurveyResourceSubmitStatus(int resourceId, int userId) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT submitted FROM `lw_survey_resource_user` WHERE resource_id = ? AND user_id = ?")) {
            ps.setInt(1, resourceId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;
        }
    }

    /**
     * @param finalSubmit true if this is the final submit
     */
    protected void saveAnswers(SurveyUserAnswers surveyAnswer, final boolean finalSubmit) throws SQLException {
        try (PreparedStatement insert = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)")) {
            // send the two answer types to DB
            for (final Entry<Integer, String> pair : surveyAnswer.getAnswers().entrySet()) {
                try {
                    insert.setInt(1, surveyAnswer.getResourceId());
                    insert.setInt(2, surveyAnswer.getUserId());
                    insert.setInt(3, pair.getKey());
                    insert.setString(4, pair.getValue());
                    insert.executeQuery();
                } catch (Throwable e) {
                    log.error(pair.toString(), e);
                }
            }

            for (final Entry<Integer, String[]> pair : surveyAnswer.getMultipleAnswers().entrySet()) {
                insert.setInt(1, surveyAnswer.getResourceId());
                insert.setInt(2, surveyAnswer.getUserId());
                insert.setInt(3, pair.getKey());
                insert.setString(4, concatMultipleAnswers(pair.getValue()));
                insert.executeQuery();
            }
        }

        surveyAnswer.setSaved(true);

        if (finalSubmit) {
            setSurveyResourceSubmitStatus(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), true);
            surveyAnswer.setSubmitted(true);
        }
    }

    /**
     * Returns all survey resources that exists in the groups of the given course.
     */
    public List<Resource> getSurveyResourcesByUserAndCourse(int courseId) throws SQLException {
        return learnweb.getResourceManager().getSurveyResourcesByUserAndCourse(courseId);
    }

    protected Resource saveSurveyResource(SurveyResource surveyResource) throws SQLException {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_resource` (`resource_id`, `survey_id`, `open_date`, `close_date`, `editable`) VALUES (?,?,?,?,?)")) {
            replace.setInt(1, surveyResource.getId());
            replace.setInt(2, surveyResource.getSurveyId());
            replace.setTimestamp(3, SqlHelper.convertDateTime(surveyResource.getStart()));
            replace.setTimestamp(4, SqlHelper.convertDateTime(surveyResource.getEnd()));
            replace.setBoolean(5, surveyResource.isSaveable());
            replace.executeUpdate();
        }
        return surveyResource;
    }

    /**
     * @return All users who have saved the survey at least once
     */
    protected List<User> getUsersWhoSavedSurveyResource(int surveyResourceId) throws SQLException {
        return getUsers("SELECT DISTINCT user_id FROM `lw_survey_answer` WHERE `resource_id` = ?", surveyResourceId);
    }

    protected List<User> getUsersWhoSubmittedSurveyResource(int surveyResourceId) throws SQLException {
        return getUsers("SELECT user_id FROM `lw_survey_resource_user` WHERE `resource_id` = ? AND `submitted` =1", surveyResourceId);
    }

    private List<User> getUsers(String query, int parameter) throws SQLException {
        UserManager userManager = learnweb.getUserManager();
        List<User> users = new LinkedList<>();

        try (PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement(query)) {
            preparedStatement.setInt(1, parameter);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                users.add(userManager.getUser(rs.getInt(1)));
            }
        }
        return users;
    }

    /**
     * Returns 'true' if survey associated with at least one resource.
     */
    public boolean isSurveyAssociatedWithResource(int surveyId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_resource` WHERE survey_id=? LIMIT 1")) {
            select.setInt(1, surveyId);
            ResultSet rs = select.executeQuery();
            if (rs.next()) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     */
    public List<Survey> getPublicSurveysByOrganisationOrUser(User user) throws SQLException {
        List<Survey> surveys = new ArrayList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey` WHERE `organization_id` = ? and `deleted` = 0 and (`public_template` = 1 or `user_id` = ?)")) {
            select.setInt(1, user.getOrganisationId());
            select.setInt(2, user.getId());
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                surveys.add(createSurvey(rs));
            }
        }
        return surveys;
    }

    public void save(Survey survey) throws SQLException {
        save(survey, false);
    }

    /**
     * Persists the survey. Updates ids if not yet stored
     *
     * @param updateMetadataOnly performance optimization: if true only metadata like title and description will be saved but not changes to questions
     */
    protected void save(Survey survey, boolean updateMetadataOnly) throws SQLException {
        // persists metadata
        if (survey.getId() <= 0) {
            try (PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO lw_survey (organization_id, title, description, user_id, public_template) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, survey.getOrganizationId());
                insert.setString(2, survey.getTitle());
                insert.setString(3, survey.getDescription());
                insert.setInt(4, survey.getUserId());
                insert.setInt(5, survey.isPublicTemplate() ? 1 : 0);
                insert.executeUpdate();
                ResultSet rs = insert.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                survey.setId(rs.getInt(1));
            }
        } else {
            try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_survey` SET `organization_id`=?, `title`=?, `description`=?, `user_id`=?, `public_template`=? WHERE `survey_id`=?")) {
                update.setInt(1, survey.getOrganizationId());
                update.setString(2, survey.getTitle());
                update.setString(3, survey.getDescription());
                update.setInt(4, survey.getUserId());
                update.setInt(5, survey.isPublicTemplate() ? 1 : 0);
                update.setInt(6, survey.getId());
                update.executeUpdate();
            }
        }

        if (!updateMetadataOnly) {
            for (SurveyQuestion question : survey.getQuestions()) {
                question.setSurveyId(survey.getId()); // need to update id in case survey has just been created
                saveQuestion(question);
            }
        }
    }

    protected void saveQuestion(SurveyQuestion question) throws SQLException {
        if (question.getId() <= 0) {
            try (PreparedStatement insert = learnweb.getConnection().prepareStatement(
                "INSERT INTO `lw_survey_question`(`deleted`, `survey_id`, `order`, `question`, `question_type`, `option`, `info`, `required`)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, question.isDeleted() ? 1 : 0);
                insert.setInt(2, question.getSurveyId());
                insert.setInt(3, question.getOrder());
                insert.setString(4, question.getLabel());
                insert.setString(5, question.getType().toString());
                insert.setString(6, question.getOptions().values().toString().substring(1, question.getOptions().values().toString().length() - 1).replace(",", "|||").replaceAll("\\s", ""));
                insert.setString(7, question.getInfo());
                insert.setInt(8, question.isRequired() ? 1 : 0);
                insert.executeUpdate();
                ResultSet rs = insert.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                question.setId(rs.getInt(1));
            }
        } else {
            try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_survey_question` SET `deleted`=?, `survey_id`=?, `order`=?, `question`=?, `question_type`=?, `option`=?, `info`=?, `required`=? WHERE question_id=?")) {
                update.setInt(1, question.isDeleted() ? 1 : 0);
                update.setInt(2, question.getSurveyId());
                update.setInt(3, question.getOrder());
                update.setString(4, question.getLabel());
                update.setString(5, question.getType().toString());
                update.setString(6, question.getOptions().values().toString().substring(1, question.getOptions().values().toString().length() - 1).replace(",", "|||").replaceAll("\\s", ""));
                update.setString(7, question.getInfo());
                update.setInt(8, question.isRequired() ? 1 : 0);
                update.setInt(9, question.getId());
                update.executeUpdate();
            }
        }

        for (SurveyQuestionOption answer : question.getAnswers()) {
            saveQuestionOption(question.getId(), answer);
        }
    }

    private void saveQuestionOption(int questionId, SurveyQuestionOption option) throws SQLException {
        if (option.getId() != 0) {
            try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE `lw_survey_question_option` SET `value`=?, `deleted`=? WHERE `answer_id`=?")) {
                update.setString(1, option.getValue());
                update.setInt(2, option.isDeleted() ? 1 : 0);
                update.setInt(3, option.getId());
                update.executeUpdate();
            }
        } else if (!option.isDeleted()) {
            try (PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_survey_question_option` (`question_id`, `value`, `deleted`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, questionId);
                insert.setString(2, option.getValue());
                insert.setInt(3, option.isDeleted() ? 1 : 0);
                insert.executeUpdate();
                ResultSet rs = insert.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("database error: no id generated");
                }
                option.setId(rs.getInt(1));
            }
        }
    }

    protected void deleteSurvey(int surveyId) throws SQLException {
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("UPDATE `lw_survey` SET `deleted`=1 WHERE `survey_id`=?")) {
            delete.setInt(1, surveyId);
            delete.executeUpdate();
        }
    }

    private static String concatMultipleAnswers(String[] answers) {
        if (ArrayUtils.isEmpty(answers)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : answers) {
            sb.append("|||").append(s.replace("|||", "|I|"));
        }

        return sb.substring(3);
    }

}
