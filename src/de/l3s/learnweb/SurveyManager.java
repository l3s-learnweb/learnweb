package de.l3s.learnweb;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

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
    private ArrayList<SurveyMetaDataFields> formQuestions = new ArrayList<SurveyMetaDataFields>();
    private Date start;
    private Date end;
    private int survey_id;
    private int resource_id;
    private String surveyTitle;
    private String description;

    public SurveyManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public ArrayList<SurveyMetaDataFields> getFormQuestions(int resourceid)
    {
        resource_id = resourceid;
        String getSurveyId = "SELECT * FROM `lw_survey_resource` WHERE `resource_id` = ?";
        PreparedStatement ps = null;
        try
        {
            ps = learnweb.getConnection().prepareStatement(getSurveyId);
        }
        catch(SQLException e3)
        {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        try
        {
            ps.setInt(1, resourceid);
        }
        catch(SQLException e3)
        {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        ResultSet rs = null;
        try
        {
            rs = ps.executeQuery();
        }
        catch(SQLException e3)
        {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        try
        {
            rs.next();
        }
        catch(SQLException e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        try
        {
            start = rs.getDate("open_date");
            end = rs.getDate("close_date");
            survey_id = rs.getInt("survey_id");
        }
        catch(SQLException e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        String surveyDetails = "SELECT * FROM `lw_survey` WHERE `survey_id` = ?";
        try
        {
            ps = learnweb.getConnection().prepareStatement(surveyDetails);
            ps.setInt(1, survey_id);
            ResultSet details = ps.executeQuery();
            details.next();
            surveyTitle = details.getString("title");
            description = details.getString("description");
        }
        catch(SQLException e2)
        {
            log.error("Error in fetching survey details ", e2);
        }

        String query = "SELECT * FROM `lw_survey_question` WHERE `survey_id` = ?";
        PreparedStatement preparedStmnt = null;
        ResultSet result = null;
        try
        {
            preparedStmnt = learnweb.getConnection().prepareStatement(query);
            preparedStmnt.setInt(1, survey_id);
        }
        catch(SQLException e1)
        {
            log.error("Error in establishing Connection", e1);
        }
        try
        {
            result = preparedStmnt.executeQuery();

        }
        catch(SQLException e)
        {
            log.error("Error in fetching questions", e);
        }
        try
        {
            while(result.next())
            {
                SurveyMetaDataFields formQuestion = new SurveyMetaDataFields(result.getString("question"), MetadataType.valueOf(result.getString("question_type")));
                if(!result.getString("answers").isEmpty())
                {
                    String str = result.getString("answers");
                    formQuestion.setAnswers(Arrays.asList(str.split("\\s*,\\s*")));
                }
                if(!result.getString("extra").isEmpty())
                {
                    formQuestion.setExtra(result.getString("extra"));
                }
                if(!result.getString("option").isEmpty())
                {
                    String str = result.getString("option");
                    formQuestion.setOptions(Arrays.asList(str.split("\\s*,\\s*")));
                }
                if(!result.getString("info").isEmpty())
                {
                    formQuestion.setInfo(result.getString("info"));
                }
                else
                {
                    formQuestion.setInfo("");
                }
                formQuestion.setRequired(result.getBoolean("required"));
                formQuestion.setId(Integer.toString(result.getInt("question_id")));
                formQuestions.add(formQuestion);
            }
        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return formQuestions;
    }

    public void upload(int user_id, HashMap<String, String> wrappedAnswers, HashMap<String, String[]> wrappedMultipleAnswers)
    {
        String insertAnswers = "INSERT INTO `lw_survey_answer`(`resource_id`, `user_id`, `question_id`, `answer`) VALUES (?, ?, ?, ?)";

        Iterator<Entry<String, String>> answer1 = wrappedAnswers.entrySet().iterator();
        while(answer1.hasNext())
        {
            Entry<String, String> pair = answer1.next();

            try
            {
                PreparedStatement insert = learnweb.getConnection().prepareStatement(insertAnswers);
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
            System.out.println(pair1.getKey() + " " + pair1.getValue().length);
            try
            {
                PreparedStatement insert = learnweb.getConnection().prepareStatement(insertAnswers);
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
                        str = str + s + ",";
                    }
                    System.out.println("String= " + str);
                    str = str.substring(0, str.lastIndexOf(","));
                    insert.setString(4, str);
                }

                insert.executeQuery();

                FacesContext context = FacesContext.getCurrentInstance();
                context.addMessage(null, new FacesMessage("Successful entry"));
            }
            catch(SQLException e)
            {
                log.error("Error in inserting answers for survey ID= " + survey_id, e);
            }

        }
    }

    public ArrayList<SurveyMetaDataFields> getFormQuestions()
    {
        return formQuestions;
    }

    public Date getStart()
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
