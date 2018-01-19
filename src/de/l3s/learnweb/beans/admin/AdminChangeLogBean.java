package de.l3s.learnweb.beans.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.beans.ApplicationBean;

/**
 * shitty implementation.
 * if it is ever used it needs a separate DAO class
 * 
 */
@ManagedBean
@ViewScoped
public class AdminChangeLogBean extends ApplicationBean
{
    @NotEmpty
    private String message;

    public void update() throws SQLException
    {
        java.sql.PreparedStatement pstmt = getLearnweb().getConnection().prepareStatement("INSERT INTO admin_change_log (message)       VALUES (?)");
        pstmt.setString(1, message);
        pstmt.executeUpdate();
        /*getLearnweb().setAdminMessage(message);*/

    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public List<String> getChangelogmessages() throws SQLException
    {
        PreparedStatement pstmtGetChangeLog = getLearnweb().getConnection().prepareStatement("SELECT * FROM  `admin_change_log` ORDER BY  `admin_change_log`.`log_entry_num` DESC LIMIT 0 , 30");

        List<String> messages = new LinkedList<String>();
        ResultSet rs = pstmtGetChangeLog.executeQuery();
        String msg = null;
        while(rs.next())
        {
            java.sql.Timestamp ts = rs.getTimestamp(3);
            msg = rs.getString(2);
            msg = ts.toString() + " : " + msg;
            messages.add(msg);
        }
        return messages;
    }

}
