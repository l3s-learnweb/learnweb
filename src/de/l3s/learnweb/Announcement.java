package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

public class Announcement implements Serializable
{
    private static final long serialVersionUID = 4219676681480459859L;

    private int id;
    private String title;
    private String text;
    private Date date;
    private int userId;

    String DATE_FORMAT = "dd-MM-yyyy"; // dates must be formated in the frontent https://git.l3s.uni-hannover.de/Learnweb/Learnweb/wikis/JSF-Tips
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(final int userId)
    {
        this.userId = userId;
    }

    public void setDate(final Date date)
    {
        this.date = date;
    }

    public String getDate() throws ParseException //LocalDate
    {
        String s = sdf.format(date);
        return s;
    }

    public int getId()
    {
        return id;
    }

    public void setId(final int id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getText()
    {
        return text;
    }

    public void setText(final String text)
    {
        this.text = text;
    }

    @Override
    public String toString()
    {
        return "Announcement [id=" + id + ", title=" + title + ", message=" + text + ", created_at=" + date + ", userId=" + userId + "]";
    }
}
