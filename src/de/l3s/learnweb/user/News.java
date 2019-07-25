package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// TODO Matvey: Move the new classes to de.l3s.learnweb they are not related to the user
// all classes that are used in Bean fields must implement the serializable interface.
// you copied this class therefore the serialVersionUID was duplicated. This will cause problems.
public class News implements Serializable
{
    private static final long serialVersionUID = 4219676681480459859L;

    private int id;
    private String title;
    private String text;
    private Date date;
    private int user_id; // TODO Matvey use camelCase. Update the getters and setters too. We are not writing C++

    String DATE_FORMAT = "dd-MM-yyyy"; // dates must be formated in the frontent https://git.l3s.uni-hannover.de/Learnweb/Learnweb/wikis/JSF-Tips
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
        return "News [id=" + id + ", title=" + title + ", message=" + text + ", created_at=" + date + ", user_id=" + user_id + "]";
    }
}
