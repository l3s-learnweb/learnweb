package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class AssessmentManager
{
    public static Logger log = Logger.getLogger(AssessmentManager.class);
    private final Learnweb learnweb;

    public AssessmentManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public ArrayList<User> getSurveyUsers(int resourceId)
    {
        ArrayList<User> users = new ArrayList<User>();
        String getUserId = "SELECT distinct(`user_id`) FROM `lw_survey_answer` WHERE resource_id=?";
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement(getUserId);
            ps.setInt(1, resourceId);
            ResultSet userId = ps.executeQuery();
            while(userId.next())
            {
                User user = learnweb.getUserManager().getUser(userId.getInt("user_id"));
                users.add(user);
            }
        }
        catch(SQLException e)
        {
            log.error("Error in fetching users for assessment survey: " + resourceId);
        }
        return users;

    }
}
