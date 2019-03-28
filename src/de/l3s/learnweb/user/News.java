package de.l3s.learnweb.user;

import de.l3s.learnweb.Learnweb;
import org.apache.log4j.Logger;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class News
{
    private static final long serialVersionUID = -1101352995500154406L;
    private static final Logger log = Logger.getLogger(News.class);

    private int id;
    private String title;
    private String text;
    private Date date;
    private int user_id;

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

    public Date getDate()
    {
        return date;
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

    /*news page, newsBean, newsBeanManenger, таблица, футер присобачить к днищу*/

}
