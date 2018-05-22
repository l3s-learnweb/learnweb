package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;

public class GroupCategory implements Serializable
{
    private static final long serialVersionUID = 8425842551114239031L;

    private int id;
    private int courseId;
    private String title;
    private String abbreviation;

    public GroupCategory(int id, int courseId, String title, String abbreviation)
    {
        super();
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.abbreviation = abbreviation;
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

    public String getAbbreviation()
    {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation)
    {
        this.abbreviation = abbreviation;
    }

    public void save() throws SQLException
    {
        if(id < 1)
            throw new IllegalStateException("Add the category first to a course");

        Learnweb.getInstance().getGroupManager().save(this);
    }
}
