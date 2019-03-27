package de.l3s.learnweb.dashboard.activity;

import de.l3s.learnweb.dashboard.CommonDashboardUserBean;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.List;

@Named
@SessionScoped
public class ActivityDashboardUsersBean extends CommonDashboardUserBean
{
    private List<Integer> selectedUsersIds;
    private static final long serialVersionUID = 6494737525546274857L;

}
