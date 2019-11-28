package de.l3s.learnweb.resource.survey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.survey.SurveyQuestion.QuestionType;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Sql;
import de.l3s.util.bean.BeanHelper;

/**
 * @author Philipp
 *
 */
public class SurveyManager
{
    private final static Logger log = Logger.getLogger(SurveyManager.class);
    private final Learnweb learnweb;

    public SurveyManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    /**
     * Returns all answers a user has given for a particular survey resource
     *
     * @param surveyResource
     * @param userId
     * @return
     * @throws SQLException
     */
    protected SurveyUserAnswers getAnswersOfUser(final SurveyResource surveyResource, int userId) throws SQLException
    {
        Validate.notNull(surveyResource);
        Validate.isTrue(userId > 0, "The value must be greater than zero: ", userId);
        Validate.isTrue(surveyResource.getId() > 0, "The value must be greater than zero: ", surveyResource.getId());

        SurveyUserAnswers surveyAnswer = new SurveyUserAnswers(userId, surveyResource.getId());

        //Get survey data
        try(PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?"))
        {
            preparedStatement.setInt(1, surveyResource.getId());
            preparedStatement.setInt(2, userId);
            ResultSet rs = preparedStatement.executeQuery();
            while(rs.next())
            {
                surveyAnswer.setSaved(true);

                SurveyQuestion question = surveyResource.getSurvey().getQuestion(rs.getInt("question_id"));
                if(question != null)
                {
                    // distinguish questions with single and multiple answers
                    QuestionType ansType = question.getType();

                    if(ansType.equals(QuestionType.MULTIPLE_MENU) || ansType.equals(QuestionType.MANY_CHECKBOX))
                    {
                        String[] answer = StringUtils.defaultString(rs.getString("answer")).split("\\s*\\|\\|\\|\\s*");

                        surveyAnswer.getMultipleAnswers().put(rs.getInt("question_id"), answer);
                    }
                    else
                    {
                        surveyAnswer.getAnswers().put(rs.getInt("question_id"), rs.getString("answer"));
                    }
                }
            }
        }
        if(surveyAnswer.isSaved())
            surveyAnswer.setSubmitted(this.getSurveyResourceSubmitStatus(surveyResource.getId(), userId));

        return surveyAnswer;
    }

    /**
     * loads the survey metadata into the given SurveyResource
     *
     * @param resource
     * @throws SQLException
     */
    protected void loadSurveyResource(SurveyResource resource) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?"))
        {
            ps.setInt(1, resource.getId());
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                resource.setEnd(rs.getDate("close_date"));
                resource.setStart(rs.getDate("open_date"));
                resource.setSurveyId(rs.getInt("survey_id"));
                resource.setSaveable(rs.getBoolean("editable"));
            }
            else
                log.error("Can't load metadata of survey resource: " + resource.getId());
        }
    }

    public SurveyResource getSurveyResource(int surveyResourceId) throws SQLException
    {
        Resource resource = learnweb.getResourceManager().getResource(surveyResourceId);

        if(resource == null)
            return null;

        if(resource.getType() != ResourceType.survey)
        {
            log.error("Survey resource requested but the resource is of type " + resource.getType() + "; " + BeanHelper.getRequestSummary());
            return null;
        }

        return (SurveyResource) resource;
    }

    public Survey getSurvey(int surveyId) throws SQLException
    {
        Survey survey = new Survey();
        survey.setId(surveyId);

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey` WHERE `survey_id` = ?"))
        {
            select.setInt(1, surveyId);
            ResultSet rs = select.executeQuery();
            if(rs.next())
            {
                survey.setTitle(rs.getString("title"));
                survey.setDescription(rs.getString("description"));
                survey.setOrganizationId(rs.getInt("organization_id"));
            }
            else
            {
                log.warn("Can't get survey: " + surveyId);
                return null;
            }
        }

        // load survey questions
        try(PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted` = 0 ORDER BY `order`"))
        {
            preparedStatement.setInt(1, surveyId);
            ResultSet rs = preparedStatement.executeQuery();
            while(rs.next())
            {
                SurveyQuestion question = new SurveyQuestion(QuestionType.valueOf(rs.getString("question_type")));
                question.setId(rs.getInt("question_id"));
                question.setLabel(rs.getString("question"));
                question.setInfo(rs.getString("info"));
                question.setExtra(rs.getString("extra"));
                question.setRequired(rs.getBoolean("required"));

                String answers = rs.getString("answers");
                if(!StringUtils.isEmpty(answers))
                {
                    // wrap answers into SurveyQuestionAnswer object
                    question.setAnswers(Arrays.stream(answers.trim().split("\\s*\\|\\|\\|\\s*")).map(answer -> new SurveyQuestionAnswer(answer)).collect(Collectors.toList()));
                }

                String options = rs.getString("option");
                if(!StringUtils.isEmpty(options))
                {
                    question.setOptions(Arrays.asList(options.trim().split("\\s*\\|\\|\\|\\s*")));
                }

                survey.addQuestion(question);
            }
        }
        return survey;
    }

    /**
     *
     * @param resourceId
     * @param userId
     * @param submitted true if the given user has finally submitted @param resourceId
     * @throws SQLException
     */
    private void setSurveyResourceSubmitStatus(int resourceId, int userId, boolean submitted) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO lw_survey_resource_user (`resource_id`, `user_id`, `submitted`) VALUES (?,?,?)"))
        {
            ps.setInt(1, resourceId);
            ps.setInt(2, userId);
            ps.setBoolean(3, submitted);
            int affectedRows = ps.executeUpdate();

            if(affectedRows != 1 && affectedRows != 2) // 1 = inserted ; 2 = updated
                log.error(affectedRows + "; Did not store survey submit: " + ps);
        }
    }

    /**
     *
     * @param resourceId
     * @param userId
     * @return true if the given survey was submitted by the user
     * @throws SQLException
     */
    protected boolean getSurveyResourceSubmitStatus(int resourceId, int userId) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT submitted FROM `lw_survey_resource_user` WHERE resource_id = ? AND user_id = ?"))
        {
            ps.setInt(1, resourceId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                return rs.getBoolean(1);
            }
            return false;
        }
    }

    private static String concatMultipleAnswers(String[] answers)
    {
        if(ArrayUtils.isEmpty(answers))
            return "";

        String str = "";

        for(String s : answers)
        {
            str += "|||" + s.replace("|||", "|I|");
        }

        return str.substring(3);
    }

    /**
     *
     * @param surveyAnswer
     * @param finalSubmit true if this is the final submit
     * @throws SQLException
     */
    protected void saveAnswers(SurveyUserAnswers surveyAnswer, final boolean finalSubmit) throws SQLException
    {
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)"))
        {
            // send the two answer types to DB
            for(final Entry<Integer, String> pair : surveyAnswer.getAnswers().entrySet())
            {
                try
                {
                    insert.setInt(1, surveyAnswer.getResourceId());
                    insert.setInt(2, surveyAnswer.getUserId());
                    insert.setInt(3, pair.getKey());
                    insert.setString(4, pair.getValue());
                    insert.executeQuery();
                }
                catch(Throwable e)
                {
                    log.error(pair.toString(), e);
                }
            }

            for(final Entry<Integer, String[]> pair : surveyAnswer.getMultipleAnswers().entrySet())
            {
                insert.setInt(1, surveyAnswer.getResourceId());
                insert.setInt(2, surveyAnswer.getUserId());
                insert.setInt(3, pair.getKey());
                insert.setString(4, concatMultipleAnswers(pair.getValue()));
                insert.executeQuery();
            }
        }

        surveyAnswer.setSaved(true);

        if(finalSubmit)
        {
            setSurveyResourceSubmitStatus(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), true);
            surveyAnswer.setSubmitted(true);
        }
    }

    /**
     * Returns all survey resources that exists in the groups of the given course
     *
     * @param courseId
     * @return
     * @throws SQLException
     */
    public List<Resource> getSurveyResourcesByUserAndCourse(int courseId) throws SQLException
    {
        return learnweb.getResourceManager().getResources("SELECT " + ResourceManager.RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_group g USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND g.course_id=? ORDER BY r.title", null, courseId);
    }

    protected Resource saveSurveyResource(SurveyResource surveyResource) throws SQLException
    {
        try(PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_resource` (`resource_id`, `survey_id`, `open_date`, `close_date`, `editable`) VALUES (?,?,?,?,?)"))
        {
            replace.setInt(1, surveyResource.getId());
            replace.setInt(2, surveyResource.getSurveyId());
            replace.setTimestamp(3, Sql.convertDateTime(surveyResource.getStart()));
            replace.setTimestamp(4, Sql.convertDateTime(surveyResource.getEnd()));
            replace.setBoolean(5, surveyResource.isSaveable());
            replace.executeUpdate();
        }
        return surveyResource;
    }

    /**
     *
     * @param surveyResourceId
     * @return All users who have saved the survey at least once
     * @throws SQLException
     */
    protected List<User> getUsersWhoSavedSurveyResource(int surveyResourceId) throws SQLException
    {
        return getUsers("SELECT DISTINCT user_id FROM `lw_survey_answer` WHERE `resource_id` = ?", surveyResourceId);
    }

    protected List<User> getUsersWhoSubmittedSurveyResource(int surveyResourceId) throws SQLException
    {
        return getUsers("SELECT user_id FROM `lw_survey_resource_user` WHERE `resource_id` = ? AND `submitted` =1", surveyResourceId);
    }

    private List<User> getUsers(String query, int parameter) throws SQLException
    {
        UserManager userManager = learnweb.getUserManager();
        List<User> users = new LinkedList<>();

        try(PreparedStatement preparedStatement = learnweb.getConnection().prepareStatement(query))
        {
            preparedStatement.setInt(1, parameter);
            ResultSet rs = preparedStatement.executeQuery();
            while(rs.next())
            {
                users.add(userManager.getUser(rs.getInt(1)));
            }
        }
        return users;
    }

    /**
     * Persists the survey. Updates ids if not yet stored
     *
     * @param survey
     * @throws SQLException
     */
    public void save(Survey survey) throws SQLException
    {
        if(survey.getId() <= 0)
            createSurvey(survey);
        else
            updateSurvey(survey);
    }

    private void updateSurvey(Survey survey)
    {
        // TODO Auto-generated method stub

    }

    private void createSurvey(Survey survey) throws SQLException
    {
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO lw_survey (organization_id, title, description) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS))
        {
            insert.setInt(1, survey.getOrganizationId());
            insert.setString(2, survey.getTitle());
            insert.setString(3, survey.getDescription());
            insert.executeUpdate();
            ResultSet rs = insert.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            survey.setId(rs.getInt(1));
        }

        for(SurveyQuestion question : survey.getQuestions())
        {
            try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO `lw_survey_question`(`survey_id`, `question`, `question_type`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS))
            {
                insert.setInt(1, survey.getId());
                insert.setString(2, question.getInfo());
                insert.setString(3, question.getType().toString());
                insert.executeUpdate();
                ResultSet rs = insert.getGeneratedKeys();
                if(!rs.next())
                    throw new SQLException("database error: no id generated");
                question.setId(rs.getInt(1));
            }
        }
    }

}
