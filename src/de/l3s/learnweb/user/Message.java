package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;

// TODO @hulyi: this class needs to be refactored and split into a dao and pojo class
public class Message implements Comparable<Message>, Serializable {
    private static final long serialVersionUID = -5510804242529450186L;
    private static final Logger log = LogManager.getLogger(Message.class);

    private int id;
    private User fromUser;
    private User toUser;
    private String title;
    private String text;
    private boolean seen = false;
    private boolean read = false;
    private Date time;

    public void save() throws SQLException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement(
            "INSERT INTO lw_message (sender_user_id, recipient_user_id, title, text, is_seen, is_read, created_at) " + "VALUES (?,?,?,?,?,?,?)");
        stmt.setInt(1, fromUser.getId());
        stmt.setInt(2, toUser.getId());
        stmt.setString(3, title);

        //String convertedText = convertText(text);
        stmt.setString(4, text);
        stmt.setBoolean(5, seen);
        stmt.setBoolean(6, read);
        stmt.setString(7, format.format(time.getTime()));
        stmt.executeUpdate();
        stmt.close();
    }

    public void seen() throws SQLException {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_message SET is_seen=1 where message_id = ?");
        stmt.setInt(1, this.id);

        stmt.executeUpdate();
        stmt.close();
    }

    public void messageRead() throws SQLException {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_message SET is_read=1 where message_id = ?");
        stmt.setInt(1, this.id);

        stmt.executeUpdate();
        stmt.close();
    }

    @Override
    public int compareTo(Message g) {
        return (this.toString()).compareTo(g.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Message message = (Message) o;
        return id == message.id && seen == message.seen && read == message.read
            && Objects.equals(fromUser, message.fromUser) && Objects.equals(toUser, message.toUser)
            && Objects.equals(title, message.title) && Objects.equals(text, message.text)
            && Objects.equals(time, message.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromUser, toUser, title, text, seen, read, time);
    }

    @Override
    public String toString() {
        String s = fromUser.getUsername() + " -> " + toUser.getUsername() + "\n" + time + ": " + "\n" + title;

        return s;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isRead() {
        try {
            this.messageRead();
        } catch (SQLException e) {
            log.error("unhandled error", e);
        }
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getFormattedTime() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String s = format.format(getTime());
        return s;
    }

    public static int howManyNotSeenMessages(User user) throws SQLException {
        int count = 0;
        PreparedStatement pstmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement(
            "SELECT count(*) as xCount FROM `lw_message` WHERE recipient_user_id = ? and is_seen = 0");

        pstmtGetUsers.setInt(1, user.getId());
        ResultSet rs = pstmtGetUsers.executeQuery();

        if (rs.next()) {
            count = rs.getInt("xCount");
        }
        return count;
    }

    public static ArrayList<Message> getAllMessagesToUser(User user) throws SQLException {
        return getAllMessagesToUser(user, -1);

    }

    public static ArrayList<Message> getAllMessagesToUser(User user, int limit) throws SQLException {
        ArrayList<Message> messageList = new ArrayList<>();
        if (user == null) {
            return messageList;
        }

        String limitStr = limit <= 0 ? "" : " limit " + limit;

        PreparedStatement stmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_message` WHERE recipient_user_id = ? order by created_at desc" + limitStr);

        stmtGetUsers.setInt(1, user.getId());
        ResultSet rs = stmtGetUsers.executeQuery();
        Message message;

        User toUser = null;
        while (rs.next()) {
            message = new Message();
            message.setId(rs.getInt("message_id"));
            UserManager um = Learnweb.getInstance().getUserManager();
            User fromUser = um.getUser(rs.getInt("sender_user_id"));
            message.setFromUser(fromUser);
            if (toUser == null) {
                toUser = um.getUser(rs.getInt("recipient_user_id"));
            }
            message.setToUser(toUser);
            message.setTitle(rs.getString("title"));
            message.setText(rs.getString("text"));
            message.setSeen(rs.getBoolean("is_seen"));
            message.setRead(rs.getBoolean("is_read"));
            message.setTime(rs.getTimestamp("created_at"));

            messageList.add(message);
        }
        stmtGetUsers.close();

        return messageList;
    }

    public static ArrayList<Message> getAllMessagesFromUser(User user) throws SQLException {
        ArrayList<Message> messageList = new ArrayList<>();
        if (user == null) {
            return messageList;
        }

        PreparedStatement stmtGetUsers = Learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_message` WHERE sender_user_id = ? order by created_at desc");

        stmtGetUsers.setInt(1, user.getId());
        ResultSet rs = stmtGetUsers.executeQuery();
        Message message;

        User toUser = null;
        while (rs.next()) {
            message = new Message();
            message.setId(rs.getInt("message_id"));
            UserManager um = Learnweb.getInstance().getUserManager();
            User fromUser = um.getUser(rs.getInt("sender_user_id"));
            message.setFromUser(fromUser);
            if (toUser == null) {
                toUser = um.getUser(rs.getInt("recipient_user_id"));
            }
            message.setToUser(toUser);
            message.setTitle(rs.getString("title"));
            message.setText(rs.getString("text"));
            message.setSeen(rs.getBoolean("is_seen"));
            message.setRead(rs.getBoolean("is_read"));
            message.setTime(rs.getTimestamp("created_at"));

            messageList.add(message);
        }
        stmtGetUsers.close();

        return messageList;
    }

    public static void setAllMessagesSeen(int userId) throws SQLException {
        PreparedStatement stmt = Learnweb.getInstance().getConnection().prepareStatement("UPDATE lw_message SET is_seen=1 where recipient_user_id = ?");
        stmt.setInt(1, userId);

        stmt.executeUpdate();
        stmt.close();
    }

}
