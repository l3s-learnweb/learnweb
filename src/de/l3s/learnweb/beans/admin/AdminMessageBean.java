package de.l3s.learnweb.beans.admin;

import java.sql.SQLException;

import javax.faces.bean.ManagedBean;

import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean

public class AdminMessageBean extends ApplicationBean
{

    @NotEmpty
    private String message;

    public AdminMessageBean() throws SQLException
    {
        message = getLearnweb().getAdminMessage();
    }

    public void update() throws SQLException
    {
        java.sql.PreparedStatement pstmt = getLearnweb().getConnection().prepareStatement("UPDATE lw_admin_message SET message=?");
        pstmt.setString(1, message);
        pstmt.executeUpdate();
        getLearnweb().setAdminMessage(message);

    }

    public void clear() throws SQLException
    {
        java.sql.PreparedStatement pstmt = getLearnweb().getConnection().prepareStatement("UPDATE lw_admin_message SET message=''");
        message = "";
        pstmt.executeUpdate();
        getLearnweb().setAdminMessage(message);

    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

}
