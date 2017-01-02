package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.Comment;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@SessionScoped
public class AdminTextAnalysisBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 5584983377737726111L;

    private String textBR;
    private String textNL;
    private int commentCount = 0;

    public String getTextBR()
    {
        return textBR;
    }

    public String getTextNL()
    {
        return textNL;
    }

    public int getCommentCount()
    {
        return commentCount;
    }

    public TreeSet<Integer> getSelectedUsers() throws SQLException
    {
        commentCount = 0;

        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_WARN, "select_user");
            return null;
        }

        TreeSet<Integer> selectedUsersSet = new TreeSet<Integer>();
        for(String userId : tempSelectedUsers)
        {
            selectedUsersSet.add(Integer.parseInt(userId));

        }

        return selectedUsersSet;
    }

    public void onAnalyseComments()
    {
        try
        {
            TreeSet<Integer> selectedUsers = getSelectedUsers();

            if(null == selectedUsers)
                return;

            List<Comment> comments = getLearnweb().getResourceManager().getCommentsByUserIds(selectedUsers);

            commentCount = comments.size();

            StringBuffer sbNL = new StringBuffer();
            StringBuffer sbBR = new StringBuffer();
            for(Comment comment : comments)
            {
                sbNL.append(comment.getText());
                sbNL.append("\n");
                //sbNL.append("\n-----\n");

                sbBR.append(comment.getText());
                sbBR.append("<br/>");
            }

            textNL = sbNL.toString();
            textBR = sbBR.toString();

        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }
}
