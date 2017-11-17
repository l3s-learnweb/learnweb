package de.l3s.learnweb;

import java.util.Date;

public class Submission
{
    int id = -1;
    int courseId;
    String title;
    String description;
    Date openDatetime = new Date();
    Date closeDatetime = new Date();
    int noOfResources = 3; //Default max no. of resources 3

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
}
