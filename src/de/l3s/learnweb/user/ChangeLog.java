package de.l3s.learnweb.user;

import de.l3s.learnweb.Learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ChangeLog
{
    private int id;
    private String title;
    private String text;

    public void save() throws SQLException{
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO lw_news (title, message, user_id) " + "VALUES (?,?,?)");
        stmt.setString(1, title);
        stmt.setString(2, text);
        stmt.setInt(3, id);
        stmt.executeUpdate();
        stmt.close();
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

}
