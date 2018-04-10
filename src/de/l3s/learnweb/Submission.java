package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.SubmissionManager.SubmittedResources;

public class Submission implements Serializable
{
    private final static long serialVersionUID = -3143872721852606973L;
    private final static Logger log = Logger.getLogger(Submission.class);

    private int id = -1;
    private int courseId;
    private String title;
    private String description;
    private Date openDatetime = new Date();
    private Date closeDatetime = new Date();
    private int noOfResources = 3; //Default max no. of resources 3

    //Fields to handle link display based on survey submitted or not
    private String surveyURL;
    private int surveyResourceId = -1;
    private Survey surveyResource;
    private boolean surveyMandatory = false;
    private boolean submitted = false;
    private List<Resource> submittedResources;

    private transient Course course;
    private transient List<SubmittedResources> submittedResourcesGroupedByUser;

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
        Calendar c = Calendar.getInstance();
        c.setTime(closeDatetime);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        this.closeDatetime = c.getTime();
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

        if(surveyResourceId > 0 && surveyResource == null)
        {
            try
            {
                surveyResource = Learnweb.getInstance().getSurveyManager().getSurveyByUserId(surveyResourceId, userId);
            }
            catch(SQLException e)
            {
                log.error("Error while fetching survey resource " + getSurveyResourceId() + "for submission " + getId(), e);
            }
        }

        if(surveyResource != null)
            return this.surveyResource.isSubmitted();

        return false;
    }

    /**
     *
     * @return The submitted resource of one particular user
     */
    public List<Resource> getSubmittedResources()
    {
        return submittedResources;
    }

    /**
     * Return all resources submitted for this submission form grouped by user
     *
     * @return
     * @throws SQLException
     */
    public List<SubmittedResources> getSubmittedResourcesGroupedByUser() throws SQLException
    {
        if(submittedResourcesGroupedByUser == null)
            submittedResourcesGroupedByUser = Learnweb.getInstance().getSubmissionManager().getSubmittedResourcesGroupedByUser(id);
        return submittedResourcesGroupedByUser;
    }

    public void setSubmittedResources(List<Resource> submittedResources)
    {
        this.submittedResources = submittedResources;
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

    public Course getCourse()
    {
        if(course == null && courseId > 0)
            course = Learnweb.getInstance().getCourseManager().getCourseById(courseId);
        return course;
    }

}
