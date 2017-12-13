package de.l3s.learnweb;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class Submission
{
    private final static Logger log = Logger.getLogger(Submission.class);
    int id = -1;
    int courseId;
    String title;
    String description;
    Date openDatetime = new Date();
    Date closeDatetime = new Date();
    int noOfResources = 3; //Default max no. of resources 3

    //Fields to handle link display based on survey submitted or not
    String surveyURL;
    int surveyResourceId = -1;
    Survey surveyResource;
    boolean surveyMandatory = false;

    List<Resource> submittedResources;

    public Submission()
    {

    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getCourseId()
    {
        return courseId;
    }

    public void setCourseId(int courseId)
    {
        this.courseId = courseId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getOpenDatetime()
    {
        return openDatetime;
    }

    public void setOpenDatetime(Date openDatetime)
    {
        this.openDatetime = openDatetime;
    }

    public Date getCloseDatetime()
    {
        return closeDatetime;
    }

    public void setCloseDatetime(Date closeDatetime)
    {
        this.closeDatetime = closeDatetime;
    }

    public int getNoOfResources()
    {
        return noOfResources;
    }

    public void setNoOfResources(int noOfResources)
    {
        this.noOfResources = noOfResources;
    }

    public void setNoOfResources(String noOfResources)
    {
        if(noOfResources != null)
            this.noOfResources = Integer.parseInt(noOfResources);
    }

    public boolean isPastSubmission()
    {
        Date today = new Date();
        return closeDatetime.before(today);
    }

    public boolean isCurrentSubmission()
    {
        Date today = new Date();
        return !openDatetime.after(today) && !closeDatetime.before(today);
    }

    public boolean isFutureSubmission()
    {
        Date today = new Date();
        return openDatetime.after(today);
    }

    public boolean isSurveyMandatory()
    {
        return surveyMandatory;
    }

    public void setSurveyMandatory(boolean surveyMandatory)
    {
        this.surveyMandatory = surveyMandatory;
        if(!surveyMandatory)
            this.surveyResourceId = -1;
    }

    public String getSurveyURL()
    {
        return surveyURL;
    }

    public void setSurveyURL(String surveyURL)
    {
        this.surveyURL = surveyURL;
        String[] surveyURLSplit = surveyURL.split("&resource_id=");
        if(surveyURLSplit.length == 2 && surveyURLSplit[1] != null)
        {
            this.surveyResourceId = Integer.parseInt(surveyURLSplit[1]);
        }
    }

    public int getSurveyResourceId()
    {
        return surveyResourceId;
    }

    public void setSurveyResourceId(int surveyResourceId)
    {
        this.surveyResourceId = surveyResourceId;
        if(this.surveyResourceId > 0)
            this.surveyMandatory = true;
    }

    public boolean isSurveySubmitted(int userId)
    {

        if(!surveyMandatory)
            return true;

        if(this.surveyResource != null)
            return this.surveyResource.isSubmitted();

        if(surveyResourceId > 0)
        {
            try
            {
                this.surveyResource = Learnweb.getInstance().getSurveyManager().getSurveyByUserId(surveyResourceId, userId);
                return this.surveyResource.isSubmitted();
            }
            catch(SQLException e)
            {
                log.error("Error while fetching survey resource " + getSurveyResourceId() + "for submission " + getId(), e);
                return false;
            }
        }

        return false;
    }

    public List<Resource> getSubmittedResources()
    {
        return submittedResources;
    }

    public void setSubmittedResources(List<Resource> submittedResources)
    {
        this.submittedResources = submittedResources;
    }
}
