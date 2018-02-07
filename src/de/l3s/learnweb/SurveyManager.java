package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.SurveyMetaDataFields.MetadataType;

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

    public Survey getAssessmentFormDetails(int resource_id, int userId) throws SQLException
    {
        HashMap<String, String> wrappedAnswers = new HashMap<String, String>();
        HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
        Survey survey = new Survey();



            //Getting survey ID
            PreparedStatement preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?");

            preparedStmnt.setInt(1, resource_id);

            ResultSet result = preparedStmnt.executeQuery();

            if(result.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");
                survey.setSurveyId(result.getInt("survey_id"));

            }
            //Getting title and description
            preparedStmnt = learnweb.getConnection().prepareStatement("SELECT `title`, `description` FROM `lw_resource` WHERE `resource_id` = ?");
            preparedStmnt.setInt(1, resource_id);
            ResultSet descTitle = preparedStmnt.executeQuery();
            while(descTitle.next())
            {
                survey.setDescription(descTitle.getString("description"));
                survey.setSurveyTitle(descTitle.getString("title"));
            }

            //Getting survey questions
            preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted`=0 ORDER BY `order` ASC");
            preparedStmnt.setInt(1, survey.getSurveyId());

            result = preparedStmnt.executeQuery();
            HashMap<String, SurveyMetaDataFields> formquestions = new HashMap<String, SurveyMetaDataFields>();
            while(result.next())
            {
                SurveyMetaDataFields formQuestion = new SurveyMetaDataFields(result.getString("question"), MetadataType.valueOf(result.getString("question_type")));
                if(result.getString("answers") != null)
                {
                    String str = result.getString("answers").trim();
                    formQuestion.setAnswers(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));

                }
                if(result.getString("extra") != null)
                {
                    formQuestion.setExtra(result.getString("extra"));
                }
                if(result.getString("option") != null)
                {
                    String str = result.getString("option").trim();
                    formQuestion.setOptions(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));
                }
                if(result.getString("info") != null)
                {
                    formQuestion.setInfo(result.getString("info"));
                }
                else
                {
                    formQuestion.setInfo("");
                }
                formQuestion.setRequired(result.getBoolean("required"));
                formQuestion.setId(Integer.toString(result.getInt("question_id")));
                formquestions.put(formQuestion.getId(), formQuestion);
                survey.getFormQuestions().add(formQuestion);
            }
            //Get answered results for user-wise display of results
            preparedStmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?");
            preparedStmnt.setInt(1, resource_id);
            preparedStmnt.setInt(2, userId);
            ResultSet rs = preparedStmnt.executeQuery();
            while(rs.next())
            {
                survey.setSubmitted(true);
                if(formquestions.containsKey(Integer.toString(rs.getInt("question_id"))))
                {
                    MetadataType ansType = formquestions.get(Integer.toString(rs.getInt("question_id"))).getType();
                    if(ansType.equals(MetadataType.MULTIPLE_MENU) || ansType.equals(MetadataType.MANY_CHECKBOX))
                    {
                        String[] answer;
                        /*if(rs.getString("answer").matches(("[\\w+\\|\\|\\|\\w+]")))*/
                        //answer=rs.getString("answer");
                        answer = rs.getString("answer").split("\\s*\\|\\|\\|\\s*");


                        /*else
                            answer = new String[] { rs.getString("answer") };*/
                        wrappedMultipleAnswers.put(Integer.toString(rs.getInt("question_id")), answer);

                    }
                    else
                    {
                        wrappedAnswers.put(Integer.toString(rs.getInt("question_id")), rs.getString("answer"));
                    }
                }

            }
            survey.setWrappedAnswers(wrappedAnswers);
            survey.setWrappedMultipleAnswers(wrappedMultipleAnswers);


        return survey;
    }

    public Survey getSurveyByUserId(int resourceId, int userId) throws SQLException
    {
        Survey survey = new Survey();
        survey.setResourceId(resourceId);

        String select = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(select);
        ps.setInt(1, resourceId);
        ps.setInt(2, userId);
        ResultSet rs = ps.executeQuery();
        if(rs.next())
        {
            survey.setSubmitted(true);
        }

        return survey;
    }

    public Survey getFormQuestions(int resourceId, int userId) throws SQLException
    {
        Survey survey = new Survey();
        survey.setResourceId(resourceId);
        String submitCheck = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        String titleDesc = "SELECT `title`, `description` FROM `lw_resource` WHERE `resource_id` = ?";
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        PreparedStatement ps = null;

            HashSet<Integer> surveyResources = new HashSet<Integer>();
            surveyResources.addAll(getSameSurveyResources(resourceId));
            for(int surveyResource_id : surveyResources)
            {
                ResultSet rs = null;
                ps = learnweb.getConnection().prepareStatement(submitCheck);
                ps.setInt(1, surveyResource_id);
                ps.setInt(2, userId);
                rs = ps.executeQuery();
                if(rs.next())
                {

                    survey.setSubmitted(true);

                }
            }

            ps = learnweb.getConnection().prepareStatement(getSurveyId);

            ps.setInt(1, resourceId);

            ResultSet rs = ps.executeQuery();

            if(rs.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");
                survey.setSurveyId(rs.getInt("survey_id"));


            }

            ps = learnweb.getConnection().prepareStatement(titleDesc);
            ps.setInt(1, resourceId);
            ResultSet descTitle = ps.executeQuery();
            while(descTitle.next())
            {
                survey.setDescription(descTitle.getString("description"));
                survey.setSurveyTitle(descTitle.getString("title"));
                if(survey.isSubmitted())
                {

                    survey.setFormQuestions(new ArrayList<SurveyMetaDataFields>());
                    return survey;

                }

            }

            String surveyDetails = "SELECT * FROM `lw_survey` WHERE `survey_id` = ?";

            ps = learnweb.getConnection().prepareStatement(surveyDetails);
            ps.setInt(1, survey.getSurveyId());
            ResultSet details = ps.executeQuery();
            if(details.next())
            {
                //survey.surveyTitle = details.getString("title");
                // survey.description = details.getString("description");
                survey.setOrganizationId(details.getInt("organization_id"));

            }

            String query = "SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted`=0 ORDER BY `order` ASC";
            PreparedStatement preparedStmnt = null;
            ResultSet result = null;

            preparedStmnt = learnweb.getConnection().prepareStatement(query);
            preparedStmnt.setInt(1, survey.getSurveyId());

            result = preparedStmnt.executeQuery();

            while(result.next())
            {
                SurveyMetaDataFields formQuestion = new SurveyMetaDataFields(result.getString("question"), MetadataType.valueOf(result.getString("question_type")));
                if(result.getString("answers") != null)
                {
                    String str = result.getString("answers").trim();
                    formQuestion.setAnswers(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));

                }
                if(result.getString("extra") != null)
                {
                    formQuestion.setExtra(result.getString("extra"));
                }
                if(result.getString("option") != null)
                {
                    String str = result.getString("option").trim();
                    formQuestion.setOptions(Arrays.asList(str.split("\\s*\\|\\|\\|\\s*")));
                }
                if(result.getString("info") != null)
                {
                    formQuestion.setInfo(result.getString("info"));
                }
                else
                {
                    formQuestion.setInfo("");
                }
                formQuestion.setRequired(result.getBoolean("required"));
                formQuestion.setId(Integer.toString(result.getInt("question_id")));
                survey.getFormQuestions().add(formQuestion);
            }

        return survey;
    }

    public void uploadAnswers(int user_id, HashMap<String, String> wrappedAnswers, HashMap<String, String[]> wrappedMultipleAnswers, int resource_id) throws SQLException
    {

        String submitCheck = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;

            ps = learnweb.getConnection().prepareStatement(submitCheck);
            ps.setInt(1, resource_id);
            ps.setInt(2, user_id);
            rs = ps.executeQuery();
            if(rs.next())
            {
                //prevent upload on twice dblclick
                return;
            }

        ps = learnweb.getConnection().prepareStatement(getSurveyId);

            ps.setInt(1, resource_id);

            rs = ps.executeQuery();


            if(rs.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");


            }


        String insertAnswers = "INSERT INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)";

        Iterator<Entry<String, String>> answer1 = wrappedAnswers.entrySet().iterator();
        PreparedStatement insert = null;

            insert = learnweb.getConnection().prepareStatement(insertAnswers);

        while(answer1.hasNext())
        {
            Entry<String, String> pair = answer1.next();



                insert.setInt(1, resource_id);
                insert.setInt(2, user_id);
                insert.setInt(3, Integer.parseInt(pair.getKey()));
                insert.setString(4, pair.getValue());
                insert.executeQuery();


        }
        Iterator<Entry<String, String[]>> answer2 = wrappedMultipleAnswers.entrySet().iterator();
        while(answer2.hasNext())
        {
            Entry<String, String[]> pair1 = answer2.next();



                insert.setInt(1, resource_id);
                insert.setInt(2, user_id);
                insert.setInt(3, Integer.parseInt(pair1.getKey()));

                if(ArrayUtils.isEmpty(pair1.getValue()))
                {
                    insert.setString(4, "");
                }
                else
                {
                    String str = "";

                    for(String s : pair1.getValue())
                    {
                        str = str + s + "|||";
                    }

                    str = str.substring(0, str.lastIndexOf("|||"));
                    insert.setString(4, str);
                }

                insert.executeQuery();



        }

    }

    public List<Resource> getSurveyResourcesByUserAndCourse(int userId, int courseId) throws SQLException
    {
        ArrayList<Resource> resources = new ArrayList<Resource>();

            String pStmt = "SELECT t1.resource_id FROM lw_resource t1 JOIN lw_group t2 ON t1.group_id=t2.group_id WHERE t1.type='survey' AND t1.deleted=0 AND (t2.course_id=? OR t1.owner_user_id=?) ORDER BY t1.group_id";
            PreparedStatement ps = learnweb.getConnection().prepareStatement(pStmt);
            ps.setInt(1, courseId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                Resource r = learnweb.getResourceManager().getResource(resourceId);
                resources.add(r);
            }


        return resources;
    }

    //Get questions
    public LinkedHashMap<String, String> getAnsweredQuestions(int resourceId) throws SQLException
    {
        LinkedHashMap<String, String> questions = new LinkedHashMap<String, String>();
        int surveyId;

        String getSurveyId = "SELECT `survey_id` FROM `lw_survey_resource` WHERE `resource_id`=?";
        //String getQuestionByOrder = "SELECT distinct(t2.question_id), t1.question FROM `lw_survey_question` t1, lw_survey_answer t2 where t2.question_id = t1.question_id and t1.survey_id=? and t2.resource_id=? order by `order`";
        String getQuestionByOrder = "SELECT distinct(t1.question_id), t1.question, t1.survey_id FROM `lw_survey_question` t1, lw_survey_answer t2 where (t2.question_id = t1.question_id or (t1.deleted=? and t1.question_type IN (\"INPUT_TEXT\", \"ONE_RADIO\", \"INPUT_TEXTAREA\", \"ONE_MENU\", \"ONE_MENU_EDITABLE\", \"MULTIPLE_MENU\", \"MANY_CHECKBOX\" ))) and t1.survey_id=? and t2.resource_id=? order by t1.`order`";
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
                    questions.put(Integer.toString(result.getInt("question_id")), result.getString("question"));
                }
            }



        return questions;
    }

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

                String getOtherResources = "SELECT t1.resource_id FROM `lw_resource` t1, lw_group t2, lw_survey_resource t3 WHERE t1.group_id =t2.group_id and t1.`type`=\"survey\" and t1.resource_id=t3.resource_id and t2.course_id=?  and  t3.survey_id=?";
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
    }

    public List<SurveyAnswer> getAnswerByUser(int resourceId, HashMap<String, String> question) throws SQLException
    {
        String answerByUser = "SELECT `answer` FROM `lw_survey_answer` WHERE `question_id`=? and `user_id`=? and `resource_id`=?";
        String userIds = "SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE `resource_id`=?";

        List<SurveyAnswer> answers = new ArrayList<SurveyAnswer>();

        //Find all results for survey id=idResult in the given course
        HashSet<Integer> surveyResources = new HashSet<Integer>();
        surveyResources.addAll(getSameSurveyResources(resourceId));
        for(int surveyResourceId : surveyResources)
        {

                PreparedStatement pSttmnt = learnweb.getConnection().prepareStatement(userIds);
                pSttmnt.setInt(1, surveyResourceId);
                ResultSet ids = pSttmnt.executeQuery();
                while(ids.next())
                {
                    SurveyAnswer ans = new SurveyAnswer();
                    ans.userId = Integer.toString(ids.getInt("user_id"));
                    ans.userName = learnweb.getUserManager().getUser(ids.getInt("user_id")).getUsername();
                    ans.studentId = learnweb.getUserManager().getUser(ids.getInt("user_id")).getStudentId();
                    pSttmnt = learnweb.getConnection().prepareStatement(answerByUser);
                    for(String qid : question.keySet())
                    {
                        pSttmnt.setInt(1, Integer.parseInt(qid));
                        pSttmnt.setInt(2, ids.getInt("user_id"));
                        pSttmnt.setInt(3, surveyResourceId);
                        ResultSet result = pSttmnt.executeQuery();
                        if(result.next())
                        {
                            String answerOfUser = result.getString("answer");

                            answerOfUser = answerOfUser.replaceAll("\\|\\|\\|", ",");

                            ans.answers.put(qid, answerOfUser);
                        }
                        else
                            ans.answers.put(qid, "Unanswered");
                    }
                    answers.add(ans);
                }

        }
        return answers;
    }

    public ArrayList<User> getSurveyUsers(int resourceId) throws SQLException
    {
        HashSet<Integer> surveyResources = new HashSet<Integer>();
        surveyResources = getSameSurveyResources(resourceId);
        ArrayList<User> users = new ArrayList<User>();
        for(int surveyResourceId : surveyResources)
        {
            String getUserId = "SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE resource_id=?";

                PreparedStatement ps = learnweb.getConnection().prepareStatement(getUserId);
                ps.setInt(1, surveyResourceId);
                ResultSet userId = ps.executeQuery();
                while(userId.next())
                {
                    User user = learnweb.getUserManager().getUser(userId.getInt("user_id"));
                    if(!users.contains(user))
                        users.add(user);
                }


        }
        return users;
    }

}
/*public ArrayList<SurveyMetaDataFields> getFormQuestions()
{
    return formQuestions;
}*/

/*public Date getStart()
{
    return start;
}

public void setStart(Date start)
{
    this.start = start;
}

public Date getEnd()
{
    return end;
}

public void setEnd(Date end)
{
    this.end = end;
}

public int getSurvey_id()
{
    return survey_id;
}

public void setSurvey_id(int survey_id)
{
    this.survey_id = survey_id;
}

public String getSurveyTitle()
{
    return surveyTitle;
}

public void setSurveyTitle(String surveyTitle)
{
    this.surveyTitle = surveyTitle;
}

public String getDescription()
{
    return description;
}

public void setDescription(String description)
{
    this.description = description;
}

public int getResource_id()
{
    return resource_id;
}

public void setResource_id(int resource_id)
{
    this.resource_id = resource_id;
}

}
*/
