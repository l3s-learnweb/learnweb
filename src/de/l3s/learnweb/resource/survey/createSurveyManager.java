package de.l3s.learnweb.resource.survey;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * @author Rishita
 *
 */
public class createSurveyManager
{
    public static Logger log = Logger.getLogger(createSurveyManager.class);
    private final Learnweb learnweb;

    public createSurveyManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void copySurveyResource(int oldResourceId, int newResourceId)
    {
        String selectOldData = "SELECT `resource_id`, `survey_id`, `open_date`, `close_date` FROM `lw_survey_resource` WHERE `resource_id` = ?";
        String insertCopy = "INSERT INTO `lw_survey_resource`(`resource_id`, `survey_id`, `open_date`, `close_date`) VALUES (?, ?, ?, ?)";
        try
        {
            PreparedStatement cpResource = learnweb.getConnection().prepareStatement(selectOldData);
            cpResource.setInt(1, oldResourceId);
            ResultSet oldData = cpResource.executeQuery();
            while(oldData.next())
            {
                PreparedStatement copy = learnweb.getConnection().prepareStatement(insertCopy);
                copy.setInt(1, newResourceId);
                copy.setInt(2, oldData.getInt("survey_id"));
                copy.setDate(3, oldData.getDate("open_date"));
                copy.setDate(4, oldData.getDate("close_date"));
                copy.executeUpdate();

            }
        }
        catch(SQLException e)
        {
            log.error("Error in copying survey resource for resource id: " + oldResourceId, e);
        }

    }

    public void createSurveyResource(int resourceId, String title, String desc, java.util.Date open, java.util.Date close)
    {

        try
        {
            String insertSurveyDetails = "INSERT INTO `lw_survey`(`title`, `description`, `organization_id`) VALUES (?, ?, ?)";
            String insertNewSurvey = "INSERT INTO `lw_survey_resource`(`resource_id`, `survey_id`, `open_date`, `close_date`) VALUES (?, ?, ?, ?)";

            PreparedStatement newSurvey = learnweb.getConnection().prepareStatement(insertSurveyDetails, Statement.RETURN_GENERATED_KEYS);
            newSurvey.setString(1, title);
            newSurvey.setString(2, desc);
            newSurvey.setInt(3, 0); //TODO: set organization id
            newSurvey.executeUpdate();
            ResultSet rs = newSurvey.getGeneratedKeys();
            rs.next();
            int newSurveyId = rs.getInt(1);

            // System.out.println("r id" + surveyRes.getId());
            PreparedStatement surveyResource = learnweb.getConnection().prepareStatement(insertNewSurvey);
            surveyResource.setInt(1, resourceId);
            surveyResource.setInt(2, newSurveyId);
            if(open != null && close != null)
            {
                Date newOpen = new Date(open.getTime());
                Date newClose = new Date(close.getTime());
                surveyResource.setDate(3, newOpen);
                surveyResource.setDate(4, newClose);
            }
            else
            {
                surveyResource.setDate(3, null);
                surveyResource.setDate(4, null);
            }
            surveyResource.executeUpdate();
            FacesContext context = FacesContext.getCurrentInstance();
            context.addMessage(null, new FacesMessage("Successful entry"));

        }
        catch(Exception e)
        {
            log.error("Error in generating survey", e);
        }

    }

}
