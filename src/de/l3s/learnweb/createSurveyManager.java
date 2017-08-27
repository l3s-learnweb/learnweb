package de.l3s.learnweb;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

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

    public void createSurveyResource(String title, String desc, java.util.Date open, java.util.Date close)
    {

        try
        {
            Resource iconResource = learnweb.getResourceManager().getResource(204095);

            Resource surveyRes = new Resource();
            surveyRes.setThumbnail0(iconResource.getThumbnail0());
            surveyRes.setThumbnail1(iconResource.getThumbnail1());
            surveyRes.setThumbnail2(iconResource.getThumbnail2());
            surveyRes.setThumbnail3(iconResource.getThumbnail3());
            surveyRes.setThumbnail4(iconResource.getThumbnail4());
            surveyRes.setDeleted(false);
            surveyRes.setSource("Survey");
            surveyRes.setType("Survey");
            surveyRes.setTitle(title);
            surveyRes.setDescription(desc);
            surveyRes.setUrl(learnweb.getServerUrl() + "/lw/showGlossary.jsf?resource_id=" + Integer.toString(surveyRes.getId()));
            surveyRes.save();
            String insertSurveyDetails = "INSERT INTO `lw_survey`(`title`, `description`, `organization_id`) VALUES (?, ?, ?)";
            String insertNewSurvey = "INSERT INTO `lw_survey_resource`(`resource_id`, `survey_id`, `open_date`, `close_date`) VALUES (?, ?, ?, ?)";

            PreparedStatement newSurv = learnweb.getConnection().prepareStatement(insertSurveyDetails, Statement.RETURN_GENERATED_KEYS);
            newSurv.setString(1, title);
            newSurv.setString(2, desc);
            newSurv.setInt(3, 0); //TODO: set organization id
            newSurv.executeUpdate();
            ResultSet rs = newSurv.getGeneratedKeys();
            rs.next();
            int newSurveyId = rs.getInt(1);

            // System.out.println("r id" + surveyRes.getId());
            PreparedStatement surveyResource = learnweb.getConnection().prepareStatement(insertNewSurvey);
            surveyResource.setInt(1, surveyRes.getId());
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
