package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    public static Logger log = Logger.getLogger(SurveyManager.class);
    private final Learnweb learnweb;
    //private ArrayList<SurveyMetaDataFields> formQuestions = new ArrayList<SurveyMetaDataFields>();

    public SurveyManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public Survey getAssessmentFormDetails(int resource_id, int userId)
    {
        HashMap<String, String> wrappedAnswers = new HashMap<String, String>();
        HashMap<String, String[]> wrappedMultipleAnswers = new HashMap<String, String[]>();
        Survey survey = new Survey();
        String assessmentAnswers = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        String titleDesc = "SELECT `title`, `description` FROM `lw_resource` WHERE `resource_id` = ?";
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        String query = "SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted`=0 ORDER BY `order` ASC";
        try
        {
            PreparedStatement preparedStmnt = null;
            ResultSet result = null;
            preparedStmnt = learnweb.getConnection().prepareStatement(getSurveyId);

            preparedStmnt.setInt(1, resource_id);

            result = preparedStmnt.executeQuery();

            if(result.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");
                survey.survey_id = result.getInt("survey_id");

                //System.out.println(survey.survey_id);
            }

            preparedStmnt = learnweb.getConnection().prepareStatement(titleDesc);
            preparedStmnt.setInt(1, resource_id);
            ResultSet descTitle = preparedStmnt.executeQuery();
            while(descTitle.next())
            {
                survey.description = descTitle.getString("description");
                survey.surveyTitle = descTitle.getString("title");
            }

            preparedStmnt = learnweb.getConnection().prepareStatement(query);
            preparedStmnt.setInt(1, survey.survey_id);

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
                survey.formQuestions.add(formQuestion);
            }
            ResultSet rs = null;
            preparedStmnt = learnweb.getConnection().prepareStatement(assessmentAnswers);
            preparedStmnt.setInt(1, resource_id);
            preparedStmnt.setInt(2, userId);
            rs = preparedStmnt.executeQuery();
            while(rs.next())
            {
                survey.submitted = true;

                MetadataType ansType = formquestions.get(Integer.toString(rs.getInt("question_id"))).getType();
                if(ansType.equals("MULTIPLE_MENU") || ansType.equals("MANY_CHECKBOX"))
                {
                    String[] answer;
                    if(rs.getString("answer").contains("|||"))
                        answer = rs.getString("answer").split("|||");
                    else
                        answer = new String[] { rs.getString("answer") };
                    wrappedMultipleAnswers.put(Integer.toString(rs.getInt("question_id")), answer);

                }
                else
                {
                    wrappedAnswers.put(Integer.toString(rs.getInt("question_id")), rs.getString("answer"));
                }
            }
            survey.wrappedAnswers = wrappedAnswers;
            survey.wrappedMultipleAnswers = wrappedMultipleAnswers;

        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return survey;
    }

    public Survey getSurveyByUserId(int resourceId, int userId) throws SQLException
    {
        Survey survey = new Survey();
        survey.setResource_id(resourceId);

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

    public Survey getFormQuestions(int resource_id, int userId)
    {
        Survey survey = new Survey();
        survey.resource_id = resource_id;
        String submitCheck = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        String titleDesc = "SELECT `title`, `description` FROM `lw_resource` WHERE `resource_id` = ?";
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        PreparedStatement ps = null;
        try
        {
            ResultSet rs = null;
            ps = learnweb.getConnection().prepareStatement(submitCheck);
            ps.setInt(1, resource_id);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            if(rs.next())
            {

                survey.submitted = true;

            }

            ps = learnweb.getConnection().prepareStatement(getSurveyId);

            ps.setInt(1, resource_id);

            rs = ps.executeQuery();

            if(rs.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");
                survey.survey_id = rs.getInt("survey_id");

                //System.out.println(survey.survey_id);
            }

            ps = learnweb.getConnection().prepareStatement(titleDesc);
            ps.setInt(1, resource_id);
            ResultSet descTitle = ps.executeQuery();
            while(descTitle.next())
            {
                survey.description = descTitle.getString("description");
                survey.surveyTitle = descTitle.getString("title");
                if(survey.isSubmitted())
                {

                    survey.formQuestions = new ArrayList<SurveyMetaDataFields>();
                    return survey;

                }

            }

            String surveyDetails = "SELECT * FROM `lw_survey` WHERE `survey_id` = ?";

            ps = learnweb.getConnection().prepareStatement(surveyDetails);
            ps.setInt(1, survey.survey_id);
            ResultSet details = ps.executeQuery();
            if(details.next())
            {
                //survey.surveyTitle = details.getString("title");
                // survey.description = details.getString("description");
                survey.organizationId = details.getInt("organization_id");

            }

            String query = "SELECT * FROM `lw_survey_question` WHERE `survey_id` = ? and `deleted`=0 ORDER BY `order` ASC";
            PreparedStatement preparedStmnt = null;
            ResultSet result = null;

            preparedStmnt = learnweb.getConnection().prepareStatement(query);
            preparedStmnt.setInt(1, survey.survey_id);

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
                survey.formQuestions.add(formQuestion);
            }
        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return survey;
    }

    public void uploadAnswers(int user_id, HashMap<String, String> wrappedAnswers, HashMap<String, String[]> wrappedMultipleAnswers, int resource_id)
    {
        int survey_id = 0;
        String submitCheck = "SELECT * FROM `lw_survey_answer` WHERE `resource_id` = ? AND `user_id` = ?";
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            ps = learnweb.getConnection().prepareStatement(submitCheck);
            ps.setInt(1, resource_id);
            ps.setInt(2, user_id);
            rs = ps.executeQuery();
            if(rs.next())
            {
                //prevent upload on twice dblclick
                return;
            }

        }
        catch(SQLException e)
        {
            log.error("Error in checking submit check on answer upload" + e);
        }
        try
        {
            ps = learnweb.getConnection().prepareStatement(getSurveyId);

            ps.setInt(1, resource_id);

            rs = ps.executeQuery();
        }
        catch(SQLException e3)
        {
            log.error("Error in getting survey id ", e3);
        }
        try
        {

            if(rs.next())
            {

                //  start = rs.getDate("open_date");
                // end = rs.getDate("close_date");
                survey_id = rs.getInt("survey_id");

            }

        }
        catch(SQLException e2)
        {
            log.error(e2);
        }
        String insertAnswers = "INSERT INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)";

        Iterator<Entry<String, String>> answer1 = wrappedAnswers.entrySet().iterator();
        PreparedStatement insert = null;
        try
        {
            insert = learnweb.getConnection().prepareStatement(insertAnswers);
        }
        catch(SQLException e1)
        {
            log.error("Error in initializing prepared Statement for answers", e1);
        }
        while(answer1.hasNext())
        {
            Entry<String, String> pair = answer1.next();

            try
            {

                insert.setInt(1, resource_id);
                insert.setInt(2, user_id);
                insert.setInt(3, Integer.parseInt(pair.getKey()));
                insert.setString(4, pair.getValue());
                insert.executeQuery();
            }
            catch(SQLException e)
            {
                log.error("Error in inserting answers for survey ID= " + survey_id, e);
            }

        }
        Iterator<Entry<String, String[]>> answer2 = wrappedMultipleAnswers.entrySet().iterator();
        while(answer2.hasNext())
        {
            Entry<String, String[]> pair1 = answer2.next();

            try
            {

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

                    str = str.substring(0, str.lastIndexOf(","));
                    insert.setString(4, str);
                }

                insert.executeQuery();

            }
            catch(SQLException e)
            {
                log.error("Error in inserting answers for survey ID= " + survey_id, e);
            }

        }

    }

    public LinkedHashMap<String, String> getAnsweredQuestions(int resourceId)
    {
        LinkedHashMap<String, String> questions = new LinkedHashMap<String, String>();
        int surveyId;

        String getSurveyId = "SELECT `survey_id` FROM `lw_survey_resource` WHERE `resource_id`=?";
        String getQuestionByOrder = "SELECT distinct(t2.question_id), t1.question FROM `lw_survey_question` t1, lw_survey_answer t2 where t2.question_id = t1.question_id and t1.survey_id=? order by `order`";

        try
        {
            PreparedStatement pSttmnt = learnweb.getConnection().prepareStatement(getSurveyId);
            pSttmnt.setInt(1, resourceId);
            ResultSet idResult = pSttmnt.executeQuery();
            if(idResult.next())
            {
                surveyId = idResult.getInt("survey_id");
                pSttmnt = learnweb.getConnection().prepareStatement(getQuestionByOrder);
                pSttmnt.setInt(1, surveyId);
                ResultSet result = pSttmnt.executeQuery();
                while(result.next())
                {
                    questions.put(Integer.toString(result.getInt("question_id")), result.getString("question"));
                }
            }
        }
        catch(SQLException e)
        {
            log.error("Error in fetching questions for survey result, resource id: " + resourceId, e);
        }

        return questions;
    }

    public List<SurveyAnswer> getAnswerByUser(int resourceId, HashMap<String, String> question)
    {
        String answerByUser = "SELECT `answer` FROM `lw_survey_answer` WHERE `question_id`=? and `user_id`=? and `resource_id`=?";
        String userIds = "SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE `resource_id`=?";

        List<SurveyAnswer> answers = new ArrayList<SurveyAnswer>();

        try
        {
            PreparedStatement pSttmnt = learnweb.getConnection().prepareStatement(userIds);
            pSttmnt.setInt(1, resourceId);
            ResultSet ids = pSttmnt.executeQuery();
            while(ids.next())
            {
                SurveyAnswer ans = new SurveyAnswer();
                ans.userId = Integer.toString(ids.getInt("user_id"));
                ans.userName = learnweb.getUserManager().getUser(ids.getInt("user_id")).getUsername();
                pSttmnt = learnweb.getConnection().prepareStatement(answerByUser);
                for(String qid : question.keySet())
                {
                    pSttmnt.setInt(1, Integer.parseInt(qid));
                    pSttmnt.setInt(2, ids.getInt("user_id"));
                    pSttmnt.setInt(3, resourceId);
                    ResultSet result = pSttmnt.executeQuery();
                    if(result.next())
                        ans.answers.put(qid, result.getString("answer"));
                    else
                        ans.answers.put(qid, "Unanswered");
                }
                answers.add(ans);
            }
        }
        catch(SQLException e)
        {
            log.error("Error in fetching answer for resource id: " + resourceId, e);
        }

        return answers;
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
