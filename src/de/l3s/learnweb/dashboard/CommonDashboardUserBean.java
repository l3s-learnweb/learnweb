package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

@Named
@SessionScoped
public class CommonDashboardUserBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = 9047964884484786815L;

    private List<Integer> selectedUsersIds = null;

    public CommonDashboardUserBean()
    {
    }

    public void onSubmitSelectedUsers()
    {

        List<Integer> newSelectedUsers = getSelectedUsers();
        if(newSelectedUsers != null)
        {
            selectedUsersIds = newSelectedUsers;

        }

    }

    private ArrayList<Integer> getSelectedUsers()
    {
        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_ERROR, "select_user");
            return null;
        }

        ArrayList<Integer> selectedUsersSet = new ArrayList<>();
        for(String userId : tempSelectedUsers)
            selectedUsersSet.add(Integer.parseInt(userId));

        return selectedUsersSet;
    }

    public ArrayList<User> getUsersList() throws SQLException
    {
        ArrayList<User> users = new ArrayList<>();
        UserManager userManager = getLearnweb().getUserManager();
        for(int userId : selectedUsersIds)
        {
            users.add(userManager.getUser(userId));
        }
        return users;
    }

    public List<Integer> getSelectedUsersIds()
    {
        return selectedUsersIds;
    }

    public void setSelectedUsersIds(List<Integer> selectedUsersIds)
    {
        this.selectedUsersIds = selectedUsersIds;
    }

}
