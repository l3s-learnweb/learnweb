package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.TreeSet;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.Comment;

@Named
@SessionScoped
public class AdminTextAnalysisBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -3957625443067966969L;

    private String textBR;
    private String textNL;
    private int commentCount = 0;
    private List<Comment> comments;

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

    public List<Comment> getComments()
    {
        return comments;
    }

    public TreeSet<Integer> getSelectedUsers()
    {
        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_WARN, "select_user");
            return null;
        }

        TreeSet<Integer> selectedUsersSet = new TreeSet<>();
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

            comments = getLearnweb().getResourceManager().getCommentsByUserIds(selectedUsers);

            commentCount = comments.size();

            StringBuilder sbNL = new StringBuilder();
            StringBuilder sbBR = new StringBuilder();
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
            addErrorMessage(e);
        }
    }
}
