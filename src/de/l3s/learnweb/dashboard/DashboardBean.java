package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.dashboard.DashboardManager.GlossaryFieldSummery;

@ManagedBean
@ViewScoped
public class DashboardBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6265758951073418345L;
    private static final Logger log = Logger.getLogger(DashboardBean.class);
    private static final String PREFERENCE_STARTDATE = "dashboard_startdate";
    private static final String PREFERENCE_ENDDATE = "dashboard_enddate";

    private Date startDate = null;
    private Date endDate = null;
    private Integer selectedUserId = null; // optionally select a single user to display

    private List<GlossaryFieldSummery> glossaryFieldSummeryPerUser;

    public DashboardBean()
    {
    }

    public void onLoad()
    {
        log.debug("onLoad");
        User user = getUser(); // the current user
        if(user == null || !user.isModerator()) // not logged in or no privileges
            return;

        try
        {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);

            String savedStartDate = getPreference(PREFERENCE_STARTDATE, Long.toString(cal.getTimeInMillis())); // month ago
            String savedEndDate = getPreference(PREFERENCE_ENDDATE, Long.toString(new Date().getTime()));
            startDate = new Date(Long.parseLong(savedStartDate));
            endDate = new Date(Long.parseLong(savedEndDate));

            List<Integer> selectedUserIds = getUser().getOrganisation().getUserIds();
            log.debug("users: " + selectedUserIds);

            DashboardManager dashboardManager = getLearnweb().getDashboardManager();

            glossaryFieldSummeryPerUser = dashboardManager.getGlossaryFieldSummeryPerUser(selectedUserIds, startDate, endDate);
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;

        setPreference(PREFERENCE_STARTDATE, Long.toString(startDate.getTime()));
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;

        setPreference(PREFERENCE_ENDDATE, Long.toString(endDate.getTime()));
    }

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser()
    {
        return glossaryFieldSummeryPerUser;
    }

    public Integer getSelectedUserId()
    {

        return selectedUserId;
    }

    public void setSelectedUserId(Integer selectedUserId)
    {
        log.debug("getSelectedUserId: " + selectedUserId);
        this.selectedUserId = selectedUserId;
    }

}
