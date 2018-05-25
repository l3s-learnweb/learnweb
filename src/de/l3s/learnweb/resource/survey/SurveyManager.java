package de.l3s.learnweb.resource.survey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.survey.SurveyMetaDataFields.MetadataType;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.Sql;

/**
 * @author Rishita
 *
 */
public class SurveyManager
{
    private final static Logger log = Logger.getLogger(SurveyManager.class);
    private final Learnweb learnweb;
    //private ArrayList<SurveyMetaDataFields> formQuestions = new ArrayList<SurveyMetaDataFields>();

    public SurveyManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    protected SurveyUserAnswers getAnswersOfUser(Survey survey, int surveyResourceId, int userId) throws SQLException
    {
        Validate.isTrue(userId > 0, "The value must be greater than zero: ", userId);
        Validate.isTrue(surveyResourceId > 0, "The value must be greater than zero: ", userId);
        Validate.notNull(survey);

        SurveyUserAnswers surveyAnswer = new SurveyUserAnswers(userId, surveyResourceId);

        // TODO move map to survey class
        HashMap<Integer, SurveyMetaDataFields> formquestions = new HashMap<>();
        for(SurveyMetaDataFields question : survey.getQuestions())
        {
            formquestions.put(question.getId(), question);
        }

        //Get answered results for user-wise display of results

        //Get survey data
        PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?");
        preparedStmnt.setInt(1, surveyResourceId);
        preparedStmnt.setInt(2, userId);
        ResultSet rs = preparedStmnt.executeQuery();
        while(rs.next())
        {
            surveyAnswer.setSaved(true);

            int questionId = rs.getInt("question_id");

            if(formquestions.containsKey(questionId))
            {
                // distinguish simple and multiple answers
                MetadataType ansType = formquestions.get(questionId).getType();

                if(ansType.equals(MetadataType.MULTIPLE_MENU) || ansType.equals(MetadataType.MANY_CHECKBOX))
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

        if(surveyAnswer.isSaved())
            surveyAnswer.setSubmitted(this.getSurveyResourceSubmitStatus(surveyResourceId, userId));

        return surveyAnswer;
    }

    //Get questions
    @Deprecated
    public LinkedHashMap<Integer, String> getAnsweredQuestions(int resourceId) throws SQLException
    {
        LinkedHashMap<Integer, String> questions = new LinkedHashMap<>();
        int surveyId;

        String getSurveyId = "SELECT `survey_id` FROM `lw_survey_resource` WHERE `resource_id`=?";

        String getQuestionByOrder = "SELECT distinct(r.question_id), r.question, r.survey_id FROM `lw_survey_question` r, lw_survey_answer t2 where (t2.question_id = r.question_id or (r.deleted=? and r.question_type IN (\"INPUT_TEXT\", \"ONE_RADIO\", \"INPUT_TEXTAREA\", \"ONE_MENU\", \"ONE_MENU_EDITABLE\", \"MULTIPLE_MENU\", \"MANY_CHECKBOX\" ))) and r.survey_id=? and t2.resource_id=? order by r.`order`";
        //Check if all questions are fetched.

        PreparedStatement pSttmnt = learnweb.getConnection().prepareStatement(getSurveyId);
        pSttmnt.setInt(1, resourceId);
        ResultSet idResult = pSttmnt.executeQuery();
        if(idResult.next())
        {
            surveyId = idResult.getInt("survey_id");

            pSttmnt = learnweb.getConnection().prepareStatement(getQuestionByOrder);
            pSttmnt.setBoolean(1, false);
            pSttmnt.setInt(2, surveyId);
            pSttmnt.setInt(3, resourceId);
            ResultSet result = pSttmnt.executeQuery();
            while(result.next())
            {
                questions.put(result.getInt("question_id"), result.getString("question"));
            }
        }

        return questions;
    }

    //Same surevy resources in a course to merge answers
    /*
    public HashSet<Integer> getSameSurveyResources(int resourceId) throws SQLException
    {
        HashSet<Integer> surveyResources = new HashSet<Integer>();
        surveyResources.add(resourceId);
        Resource surveyResource;
        int groupId;

        int surveyId = 0;

        surveyResource = learnweb.getResourceManager().getResource(resourceId);

        groupId = surveyResource.getGroupId();
        //Fetching course id for given resource
        //Fetching other resource Ids with same survey id in the current course.
        if(groupId > 0)
        {
            int courseId = learnweb.getGroupManager().getGroupById(groupId).getCourseId();

            String getOtherResources = "SELECT r.resource_id FROM `lw_resource` r, lw_group t2, lw_survey_resource t3 WHERE r.group_id =t2.group_id and r.`type`='survey' and r.resource_id=t3.resource_id and t2.course_id=?  and  t3.survey_id=?";
            String getSurveyId = "SELECT `survey_id` FROM `lw_survey_resource` WHERE `resource_id`=?";
            PreparedStatement ps = learnweb.getConnection().prepareStatement(getSurveyId);
            ps.setInt(1, resourceId);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                surveyId = rs.getInt("survey_id");
                ps = learnweb.getConnection().prepareStatement(getOtherResources);
                ps.setInt(1, courseId);
                ps.setInt(2, surveyId);
                rs = ps.executeQuery();
                while(rs.next())
                {
                    surveyResources.add(rs.getInt("resource_id"));
                }
            }
        }

        return surveyResources;
    }*/

    // TODO this method is extremely inefficient
    public List<SurveyUserAnswers> getAnswerOfAllUserForSurveyResource(int surveyResourceId, HashMap<Integer, String> question) throws SQLException
    {
        List<SurveyUserAnswers> answers = new ArrayList<SurveyUserAnswers>();

        PreparedStatement answerSelect = learnweb.getConnection().prepareStatement("SELECT `answer` FROM `lw_survey_answer` WHERE `question_id`=? and `user_id`=? and `resource_id`=?");

        PreparedStatement userSelect = learnweb.getConnection().prepareStatement("SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE `resource_id`=?");
        userSelect.setInt(1, surveyResourceId);
        ResultSet ids = userSelect.executeQuery();
        while(ids.next())
        {
            SurveyUserAnswers userServeyAnswers = new SurveyUserAnswers(ids.getInt("user_id"), surveyResourceId);
            HashMap<Integer, String> ans = userServeyAnswers.getAnswers();

            // TODO this is extremely inefficient
            for(Integer qid : question.keySet())
            {
                answerSelect.setInt(1, qid);
                answerSelect.setInt(2, ids.getInt("user_id"));
                answerSelect.setInt(3, surveyResourceId);
                ResultSet result = answerSelect.executeQuery();
                if(result.next())
                {
                    String answerOfUser = result.getString("answer");

                    answerOfUser = answerOfUser == null ? "" : answerOfUser.replaceAll("\\|\\|\\|", ",");

                    ans.put(qid, answerOfUser);

                    userServeyAnswers.setSaved(true);
                }
                else
                {
                    if(!ans.containsKey(qid))
                        ans.put(qid, "Unanswered");
                }
            }

            if(userServeyAnswers.isSaved())
                userServeyAnswers.setSubmitted(this.getSurveyResourceSubmitStatus(surveyResourceId, ids.getInt("user_id")));

            answers.add(userServeyAnswers);
        }
        return answers;
    }

    @Deprecated
    public ArrayList<User> getSurveyUsers(int surveyResourceId) throws SQLException
    {

        ArrayList<User> users = new ArrayList<>();

        PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE resource_id=?");
        ps.setInt(1, surveyResourceId);
        ResultSet userId = ps.executeQuery();
        while(userId.next())
        {
            User user = learnweb.getUserManager().getUser(userId.getInt("user_id"));
            users.add(user);
        }
        return users;
    }

    // new and refactored methods:

    /**
     * loads the survey metadata into the given SurveyResource
     *
     * @param resource
     * @throws SQLException
     */
    public void loadSurveyResource(SurveyResource resource) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?");)
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

    public Survey getSurvey(int surveyId) throws SQLException
    {
        // TODO add survey cache
        Survey survey = new Survey();
        survey.setId(surveyId);

        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey` WHERE `survey_id` = ?");)
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
        try(PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted` = 0 ORDER BY `order`");)
        {
            preparedStmnt.setInt(1, surveyId);
            ResultSet rs = preparedStmnt.executeQuery();
            while(rs.next())
            {
                SurveyMetaDataFields formQuestion = new SurveyMetaDataFields(rs.getString("question"), MetadataType.valueOf(rs.getString("question_type")));
                if(rs.getString("answers") != null)
                {
                    String str = rs.getString("answers").trim();
                    formQuestion.setAnswers(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));

                }
                if(rs.getString("extra") != null)
                {
                    formQuestion.setExtra(rs.getString("extra"));
                }
                if(rs.getString("option") != null)
                {
                    String str = rs.getString("option").trim();
                    formQuestion.setOptions(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));
                }
                if(rs.getString("info") != null)
                {
                    formQuestion.setInfo(rs.getString("info"));
                }
                else
                {
                    formQuestion.setInfo("");
                }
                formQuestion.setRequired(rs.getBoolean("required"));
                formQuestion.setId(rs.getInt("question_id"));
                survey.getQuestions().add(formQuestion);
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
    private void setSurveyResourceSubmitStauts(int resourceId, int userId, boolean submitted) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO lw_survey_resource_user (`resource_id`, `user_id`, `submitted`) VALUES (?,?,?)");)
        {
            ps.setInt(1, resourceId);
            ps.setInt(2, userId);
            ps.setBoolean(3, submitted);
            int affectedRows = ps.executeUpdate();

            if(affectedRows != 1)
                log.error(affectedRows + "; Did not store survey submit: " + ps);
        }
    }

    /**
     *
     * @param resourceId
     * @param userId
     * @return
     * @throws SQLException
     */
    private boolean getSurveyResourceSubmitStatus(int resourceId, int userId) throws SQLException
    {
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT submitted FROM `lw_survey_resource_user` WHERE resource_id = ? AND user_id = ?");)
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
     * @param finalSubmit true if thisis the final submit
     * @throws SQLException
     */
    public void saveAnswers(SurveyUserAnswers surveyAnswer, final boolean finalSubmit) throws SQLException
    {
        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)");)
        {
            // send the two answer types to DB
            Iterator<Entry<Integer, String>> answer1 = surveyAnswer.getAnswers().entrySet().iterator();
            while(answer1.hasNext())
            {
                Entry<Integer, String> pair = answer1.next();

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

            Iterator<Entry<Integer, String[]>> answer2 = surveyAnswer.getMultipleAnswers().entrySet().iterator();
            while(answer2.hasNext())
            {
                Entry<Integer, String[]> pair = answer2.next();

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
            setSurveyResourceSubmitStauts(surveyAnswer.getResourceId(), surveyAnswer.getUserId(), true);
            surveyAnswer.setSubmitted(true);
        }
    }

    /**
     * Returns all survey resources that exists in the groups of the given course
     *
     * @param userId
     * @param courseId
     * @return
     * @throws SQLException
     */
    public List<Resource> getSurveyResourcesByUserAndCourse(int courseId) throws SQLException
    {

        return learnweb.getResourceManager().getResources("SELECT " + ResourceManager.RESOURCE_COLUMNS + " FROM lw_resource r JOIN lw_group g USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND g.course_id=? ORDER BY r.title", null, courseId);
        /*
        ArrayList<Resource> resources = new ArrayList<Resource>();
        
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT r.resource_id FROM lw_resource r JOIN lw_group t2 USING(group_id) WHERE r.type='survey' AND r.deleted=0 AND t2.course_id=? ORDER BY r.title");)
        {
            ps.setInt(1, courseId);
        
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                Resource r = learnweb.getResourceManager().getResource(resourceId);
                resources.add(r);
            }
        }
        return resources;
        */
    }

    public Resource saveSurveyResource(SurveyResource surveyResource) throws SQLException
    {
        try(PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_survey_resource` (`resource_id`, `survey_id`, `open_date`, `close_date`, `editable`) VALUES (?,?,?,?,?)");)
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

    public List<User> getUsersWhoSavedSurveyResource(int surveyResourceId) throws SQLException
    {
        UserManager userManager = learnweb.getUserManager();
        List<User> users = new LinkedList<>();

        try(PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT DISTINCT user_id FROM `lw_survey_answer` WHERE `resource_id` = ?");)
        {
            preparedStmnt.setInt(1, surveyResourceId);
            ResultSet rs = preparedStmnt.executeQuery();
            while(rs.next())
            {
                users.add(userManager.getUser(rs.getInt(1)));
            }
        }
        return users;
    }

}
