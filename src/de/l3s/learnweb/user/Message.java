package de.l3s.learnweb.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

// TODO this class needs to be refactored and split into a dao and pojo class
public class Message implements Comparable<Message>
{

    private int id;
    private User fromUser;
    private User toUser;
    private String title;
    private String text;
    private boolean seen = false;
    private boolean read = false;
    private Date time;

    public static int howManyNotSeenMessages(User user) throws SQLException
    {
        int count = 0;
        PreparedStatement pstmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement("SELECT count(*) as xCount FROM `message` WHERE to_user = ? and m_seen = 0");

        pstmtGetUsers.setInt(1, user.getId());
        ResultSet rs = pstmtGetUsers.executeQuery();

        if(rs.next())
        {
            count = rs.getInt("xCount");
        }
        return count;
    }

    public static ArrayList<Message> getAllMessagesToUser(User user) throws SQLException
    {
        ArrayList<Message> messageList = new ArrayList<>();
        if(user == null)
            return messageList;

        PreparedStatement stmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `message` WHERE to_user = ? order by m_time desc");

        stmtGetUsers.setInt(1, user.getId());
        ResultSet rs = stmtGetUsers.executeQuery();
        Message message = null;

        User toUser = null;
        while(rs.next())
        {
            message = new Message();
            message.setId(rs.getInt("message_id"));
            UserManager um = Learnweb.getInstance().getUserManager();
            User fromUser = um.getUser(rs.getInt("from_user"));
            message.setFromUser(fromUser);
            if(toUser == null)
            {
                toUser = um.getUser(rs.getInt("to_user"));
            }
            message.setToUser(toUser);
            message.setTitle(rs.getString("m_title"));
            message.setText(rs.getString("m_text"));
            message.setSeen(rs.getBoolean("m_seen"));
            message.setRead(rs.getBoolean("m_read"));
            message.setTime(rs.getTimestamp("m_time"));

            messageList.add(message);
        }
        stmtGetUsers.close();

        return messageList;
    }

    public static ArrayList<Message> getAllMessagesFromUser(User user) throws SQLException{
        ArrayList<Message> messageList = new ArrayList<>();
        if(user == null)
            return messageList;

        PreparedStatement stmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `message` WHERE from_user = ? order by m_time desc");

        stmtGetUsers.setInt(1, user.getId());
        ResultSet rs = stmtGetUsers.executeQuery();
        Message message = null;

        User toUser = null;
        while(rs.next())
        {
            message = new Message();
            message.setId(rs.getInt("message_id"));
            UserManager um = Learnweb.getInstance().getUserManager();
            User fromUser = um.getUser(rs.getInt("from_user"));
            message.setFromUser(fromUser);
            if(toUser == null)
            {
                toUser = um.getUser(rs.getInt("to_user"));
            }
            message.setToUser(toUser);
            message.setTitle(rs.getString("m_title"));
            message.setText(rs.getString("m_text"));
            message.setSeen(rs.getBoolean("m_seen"));
            message.setRead(rs.getBoolean("m_read"));
            message.setTime(rs.getTimestamp("m_time"));

            messageList.add(message);
        }
        stmtGetUsers.close();

        return messageList;
    }

    public void save() throws SQLException
    {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("INSERT INTO message (from_user, to_user, m_title, m_text, m_seen, m_read, m_time) " + "VALUES (?,?,?,?,?,?,?)");
        stmt.setInt(1, fromUser.getId());
        stmt.setInt(2, toUser.getId());
        stmt.setString(3, title);

        String convertedText = convertText(text);
        stmt.setString(4, convertedText);
        stmt.setBoolean(5, seen);
        stmt.setBoolean(6, read);
        stmt.setString(7, format.format(time.getTime()));
        stmt.executeUpdate();
        stmt.close();
    }

    public void seen() throws SQLException
    {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE message SET m_seen=1 where message_id = ?");
        stmt.setInt(1, this.id);

        stmt.executeUpdate();
        stmt.close();
    }

    public void messageRead() throws SQLException
    {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE message SET m_read=1 where message_id = ?");
        stmt.setInt(1, this.id);

        stmt.executeUpdate();
        stmt.close();
    }

    public static void setAllMessagesSeen(int userId) throws SQLException
    {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE message SET m_seen=1 where to_user = ?");
        stmt.setInt(1, userId);

        stmt.executeUpdate();
        stmt.close();
    }

    private String convertText(String text)
    {
        String replacement = "<a target=\"_blank\" href=\"../lw/link/link.jsf?link=";
        return text.replaceAll("<a href=\"", replacement);
    }

    @Override
    public int compareTo(Message g)
    {
        return (this.toString()).compareTo(g.toString());
    }

    @Override
    public String toString()
    {
        String s = fromUser.getUsername() + " -> " + toUser.getUsername() + "\n" + time + ": " + "\n" + title;

        return s;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public User getFromUser()
    {
        return fromUser;
    }

    public void setFromUser(User fromUser)
    {
        this.fromUser = fromUser;
    }

    public User getToUser()
    {
        return toUser;
    }

    public void setToUser(User toUser)
    {
        this.toUser = toUser;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public boolean isSeen()
    {
        return seen;
    }

    public void setSeen(boolean seen)
    {
        this.seen = seen;
    }

    public boolean isRead()
    {
        try
        {
            this.messageRead();
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            Logger.getLogger(Message.class).error("unhandled error", e);
        }
        return read;
    }

    public void setRead(boolean read)
    {
        this.read = read;
    }

    public Date getTime()
    {
        return time;
    }

    public void setTime(Date time)
    {
        this.time = time;
    }

    public String getFormattedTime()
    {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String s = format.format(getTime());
        return s;
    }

}
