package de.l3s.learnweb.user;

import de.l3s.learnweb.Learnweb;
import org.apache.log4j.Logger;


import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class News
{
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = Logger.getLogger(News.class);

    private int id;
    private String title;
    private String text;
    private Date date;
    private int user_id;

    String DATE_FORMAT = "dd-MM-yyyy";
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

    public int getUser_id()
    {
        return user_id;
    }

    public void setUser_id(final int user_id)
    {
        this.user_id = user_id;
    }


    public void setDate(final Date date)
    {
        this.date = date;
    }

    public String getDate() throws ParseException
    {
        String s = sdf.format(date);
        return  s;
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
        return "News [id=" + id + ", title=" + title + ", message=" + text + ", created_at=" + date + ", user_id=" + user_id + "]";
    }

    public String onSaveString()
    {
        return "News [title=" + title + ", message=" + text + ", user_id=" + user_id + "]";
    }

}
