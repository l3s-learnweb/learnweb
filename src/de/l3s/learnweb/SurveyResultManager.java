package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class SurveyResultManager
{
    public static Logger log = Logger.getLogger(SurveyResultManager.class);
    private final Learnweb learnweb;

    public SurveyResultManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public HashMap<String, String> getQuestions(int resourceId)
    {
        HashMap<String, String> questions = new HashMap<String, String>();
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

    public SurveyAnswer getAnswer(int userId, int resourceId, HashMap<String, String> question)
    {
        String answerByUser = "SELECT `answer` FROM `lw_survey_answer` WHERE `question_id`=? and `user_id`=? and `resource_id`=?";
        SurveyAnswer ans = new SurveyAnswer();
        ans.userId = Integer.toString(userId);
        try
        {
            PreparedStatement pSttmnt = learnweb.getConnection().prepareStatement(answerByUser);
            for(String qid : question.keySet())
            {
                pSttmnt.setInt(1, Integer.parseInt(qid));
                pSttmnt.setInt(2, userId);
                pSttmnt.setInt(3, resourceId);
                ResultSet result = pSttmnt.executeQuery();
                if(result.next())
                    ans.answers.put(qid, result.getString("answer"));
                else
                    ans.answers.put(qid, "NULL/Unanswered");
            }
        }
        catch(SQLException e)
        {
            log.error("Error in fetching answer for resource id: " + resourceId + "for user: " + userId, e);
        }

        return ans;
    }
}
