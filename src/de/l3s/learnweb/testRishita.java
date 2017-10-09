package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class testRishita
{
    public final static Logger log = Logger.getLogger(testRishita.class);

    public static void main(String args[]) throws ClassNotFoundException, SQLException
    {
        Learnweb learnweb = Learnweb.createInstance("http://learnweb.l3s.uni-hannover.de");
        String selectSurveyId = "SELECT `survey_id` FROM `lw_survey`";
        String selectQues = "SELECT * FROM `lw_survey_question` WHERE `survey_id`=? ORDER BY `question_id` ASC";
        String addOrder = "UPDATE `lw_survey_question` SET `order`=? WHERE `question_id`=?";
        ResultSet rs = null;
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement(selectSurveyId);
            rs = ps.executeQuery();
            while(rs.next())
            {
                int order = 1;
                ps = learnweb.getConnection().prepareStatement(selectQues);
                ps.setInt(1, rs.getInt("survey_id"));
                PreparedStatement ordering = learnweb.getConnection().prepareStatement(addOrder);
                ResultSet quest = ps.executeQuery();
                int tempOrder = 0;
                while(quest.next())
                {

                    if(quest.getInt("survey_id") == 15)
                    {

                        if(quest.getInt("question_id") == 93)
                        {
                            tempOrder = order;
                            order = order + 3;
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                        else if(quest.getInt("question_id") == 373 || quest.getInt("question_id") == 378 || quest.getInt("question_id") == 383)
                        {
                            ordering.setInt(1, tempOrder);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                            tempOrder++;
                        }
                        else
                        {
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                    }
                    else if(quest.getInt("survey_id") == 16)
                    {

                        if(quest.getInt("question_id") == 141)
                        {
                            tempOrder = order;
                            order = order + 3;
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                        else if(quest.getInt("question_id") == 374 || quest.getInt("question_id") == 379 || quest.getInt("question_id") == 384)
                        {
                            ordering.setInt(1, tempOrder);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                            tempOrder++;
                        }
                        else
                        {
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }

                    }
                    else if(quest.getInt("survey_id") == 19)
                    {

                        if(quest.getInt("question_id") == 205)
                        {
                            tempOrder = order;
                            order = order + 3;
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                        else if(quest.getInt("question_id") == 375 || quest.getInt("question_id") == 380 || quest.getInt("question_id") == 385)
                        {
                            ordering.setInt(1, tempOrder);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                            tempOrder++;
                        }
                        else
                        {
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }

                    }
                    else if(quest.getInt("survey_id") == 20)
                    {

                        if(quest.getInt("question_id") == 268)
                        {
                            tempOrder = order;
                            order = order + 3;
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                        else if(quest.getInt("question_id") == 376 || quest.getInt("question_id") == 381 || quest.getInt("question_id") == 386)
                        {
                            ordering.setInt(1, tempOrder);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                            tempOrder++;
                        }
                        else
                        {
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }

                    }
                    else if(quest.getInt("survey_id") == 21)
                    {

                        if(quest.getInt("question_id") == 331)
                        {
                            tempOrder = order;
                            order = order + 3;
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }
                        else if(quest.getInt("question_id") == 377 || quest.getInt("question_id") == 382 || quest.getInt("question_id") == 387)
                        {
                            ordering.setInt(1, tempOrder);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                            tempOrder++;
                        }
                        else
                        {
                            ordering.setInt(1, order);
                            ordering.setInt(2, quest.getInt("question_id"));
                            ordering.executeUpdate();
                        }

                    }

                    else
                    {
                        ordering.setInt(1, order);
                        ordering.setInt(2, quest.getInt("question_id"));
                        ordering.executeUpdate();
                    }
                    order++;
                }
            }
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            log.error("unhandled error", e);
        }
    }

}
